package org.llm4s.speech.provider

import sttp.client4._
import org.llm4s.speech._
import org.llm4s.speech.config.OpenAISpeechConfig
import org.llm4s.speech.model._
import ujson._
import java.nio.file.Files

/**
 * OpenAI speech client implementation (TTS and ASR)
 * 
 * @author AnshumanAI
 */
class OpenAISpeechClient(config: OpenAISpeechConfig) extends TTSClient with ASRClient {

  private val backend = DefaultSyncBackend()

  override def synthesize(
    text: String,
    options: TTSSynthesisOptions
  ): Either[SpeechError, AudioResponse] =
    try {
      val requestBody = Obj(
        "model"           -> options.model,
        "input"           -> text,
        "voice"           -> options.voice,
        "response_format" -> options.responseFormat,
        "speed"           -> options.speed
      )

      val request = basicRequest
        .post(uri"${config.baseUrl}/audio/speech")
        .body(requestBody.render())
        .header("Authorization", s"Bearer ${config.apiKey}")
        .header("Content-Type", "application/json")
        .response(asByteArray)

      val response = request.send(backend)

      response.code.code match {
        case 200 =>
          response.body match {
            case Right(audioData) =>
              Right(
                AudioResponse(
                  audioData = audioData,
                  format = options.responseFormat
                )
              )
            case Left(error) =>
              Left(SpeechUnknownError(new Exception(s"Failed to get audio data: $error")))
          }
        case statusCode =>
          Left(handleErrorResponse(statusCode, response.body.fold(identity, new String(_))))
      }
    } catch {
      case e: Exception =>
        Left(SpeechUnknownError(e))
    }

  override def transcribe(
    audioData: Array[Byte],
    options: ASRTranscriptionOptions
  ): Either[SpeechError, TranscriptionResponse] =
    try {
      // Create a temporary file for the audio data
      val tempFile = Files.createTempFile("audio", ".wav")
      try {
        Files.write(tempFile, audioData)

        val multipartParts = Seq(
          multipartFile("file", tempFile.toFile).fileName("audio.wav"),
          multipart("model", options.model),
          multipart("response_format", options.responseFormat),
          multipart("temperature", options.temperature.toString)
        ) ++ 
        options.language.map(lang => multipart("language", lang)).toSeq ++
        options.prompt.map(prompt => multipart("prompt", prompt)).toSeq

        val request = basicRequest
          .post(uri"${config.baseUrl}/audio/transcriptions")
          .multipartBody(multipartParts: _*)
          .header("Authorization", s"Bearer ${config.apiKey}")
          .response(asStringAlways)

        val response = request.send(backend)

        response.code.code match {
          case 200 =>
            options.responseFormat match {
              case "json" | "verbose_json" =>
                val responseJson = ujson.read(response.body, trace = false)
                val text         = responseJson("text").str
                val language     = responseJson.obj.get("language").map(_.str)

                Right(
                  TranscriptionResponse(
                    text = text,
                    language = language
                  )
                )
              case "text" =>
                Right(
                  TranscriptionResponse(
                    text = response.body,
                    language = None
                  )
                )
              case _ => // srt, vtt, or other formats
                Right(
                  TranscriptionResponse(
                    text = response.body,
                    language = None
                  )
                )
            }
          case statusCode =>
            Left(handleErrorResponse(statusCode, response.body))
        }
      } finally {
        // Clean up the temporary file
        Files.deleteIfExists(tempFile)
      }
    } catch {
      case e: Exception =>
        Left(SpeechUnknownError(e))
    }

  private def handleErrorResponse(statusCode: Int, responseBody: String): SpeechError = {
    val errorMessage = s"HTTP $statusCode: $responseBody"
    statusCode match {
      case 401 => SpeechAuthenticationError(errorMessage)
      case 429 => SpeechRateLimitError(errorMessage)
      case 400 => SpeechValidationError(errorMessage)
      case _   => SpeechUnknownError(new Exception(errorMessage))
    }
  }
}