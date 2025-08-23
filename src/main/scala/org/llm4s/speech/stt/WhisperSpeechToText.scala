package org.llm4s.speech.stt

import org.llm4s.error.LLMError
import org.llm4s.types.Result
import org.llm4s.speech.AudioInput

import java.nio.file.{ Files, Path }
import scala.sys.process._

/**
 * Enhanced Whisper integration via CLI (whisper.cpp or openai-whisper).
 * Supports various Whisper models and output formats.
 */
final class WhisperSpeechToText(
  command: Seq[String] = Seq("whisper"),
  model: String = "base",
  outputFormat: String = "txt"
) extends SpeechToText {
  override val name: String = "whisper-cli"

  override def transcribe(input: AudioInput, options: STTOptions): Result[Transcription] =
    try {
      val wav: Path = input match {
        case AudioInput.FileAudio(path) => path
        case AudioInput.BytesAudio(bytes, _, _) =>
          val tmp = Files.createTempFile("llm4s-whisper-", ".wav")
          Files.write(tmp, bytes)
          tmp
        case AudioInput.StreamAudio(stream, _, _) =>
          val tmp = Files.createTempFile("llm4s-whisper-", ".wav")
          Files.write(tmp, stream.readAllBytes())
          tmp
      }

      val args = buildWhisperArgs(wav, options)

      val output     = args.!!
      val transcript = parseWhisperOutput(output, options)

      Right(transcript)
    } catch {
      case e: Exception => Left(LLMError.fromThrowable(e))
    }

  private def buildWhisperArgs(inputPath: Path, options: STTOptions): Seq[String] = {
    val baseArgs = command ++ Seq(
      inputPath.toString,
      "--model",
      model,
      "--output_format",
      outputFormat
    )

    val languageArgs  = options.language.map(l => Seq("--language", l)).getOrElse(Seq.empty)
    val promptArgs    = options.prompt.map(p => Seq("--initial_prompt", p)).getOrElse(Seq.empty)
    val timestampArgs = if (options.enableTimestamps) Seq("--word_timestamps", "True") else Seq.empty

    baseArgs ++ languageArgs ++ promptArgs ++ timestampArgs
  }

  private def parseWhisperOutput(output: String, options: STTOptions): Transcription = {
    // Parse output based on format and options
    val text       = output.trim
    val confidence = extractConfidence(output)
    val timestamps = if (options.enableTimestamps) extractTimestamps(output) else Nil

    Transcription(
      text = text,
      language = options.language,
      confidence = confidence,
      timestamps = timestamps,
      meta = None
    )
  }

  private def extractConfidence(output: String): Option[Double] =
    // Whisper CLI may output confidence scores in some formats
    // This is a placeholder - actual parsing depends on Whisper version
    None

  private def extractTimestamps(output: String): List[WordTimestamp] =
    // Parse word-level timestamps if available
    // Format varies by Whisper version and output format
    Nil
}
