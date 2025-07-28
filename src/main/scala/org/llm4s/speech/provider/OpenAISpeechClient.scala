package org.llm4s.speech.provider

import requests.{Response, Session}
import org.llm4s.speech._
import org.llm4s.speech.config.OpenAISpeechConfig
import org.llm4s.speech.model._
import ujson._
import java.util.Base64

/**
 * OpenAI speech client implementation (TTS and ASR)
 * 
 * @author AnshumanAI
 */
class OpenAISpeechClient(config: OpenAISpeechConfig) extends TTSClient with ASRClient {

  private val session = Session()

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

      val response = session.post(
        s"${config.baseUrl}/audio/speech",
        data = requestBody.render(),
        headers = Map(
          "Authorization" -> s"Bearer ${config.apiKey}",
          "Content-Type"  -> "application/json"
        )
      )

      if (response.statusCode == 200) {
        val audioData = response.bytes
        Right(
          AudioResponse(
            audioData = audioData,
            format = options.responseFormat
          )
        )
      } else {
        handleErrorResponse(response)
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
      val base64Audio = Base64.getEncoder.encodeToString(audioData)

      val requestBody = Obj(
        "model"           -> options.model,
        "file"            -> base64Audio,
        "response_format" -> options.responseFormat,
        "temperature"     -> options.temperature
      )

      val response = session.post(
        s"${config.baseUrl}/audio/transcriptions",
        data = requestBody.render(),
        headers = Map(
          "Authorization" -> s"Bearer ${config.apiKey}",
          "Content-Type"  -> "application/json"
        )
      )

      if (response.statusCode == 200) {
        val responseJson = ujson.read(response.text(), trace = false)
        val text         = responseJson("text").str
        val language     = responseJson.obj.get("language").map(_.str)

        Right(
          TranscriptionResponse(
            text = text,
            language = language
          )
        )
      } else {
        handleErrorResponse(response)
      }
    } catch {
      case e: Exception =>
        Left(SpeechUnknownError(e))
    }

  private def handleErrorResponse(response: Response): Either[SpeechError, Nothing] = {
    val errorMessage = s"HTTP ${response.statusCode}: ${response.text()}"
    response.statusCode match {
      case 401 => Left(SpeechAuthenticationError(errorMessage))
      case 429 => Left(SpeechRateLimitError(errorMessage))
      case 400 => Left(SpeechValidationError(errorMessage))
      case _   => Left(SpeechUnknownError(new Exception(errorMessage)))
    }
  }
}