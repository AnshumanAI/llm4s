package org.llm4s.speech.tts

import org.llm4s.error.LLMError
import org.llm4s.types.Result
import org.llm4s.speech.{ GeneratedAudio, AudioMeta }
import cats.implicits._

import java.nio.file.Files
import scala.sys.process._

/**
 * Tacotron2 integration via CLI or local server. This is a thin adapter;
 * actual model hosting is assumed external.
 */
final class Tacotron2TextToSpeech(
  command: Seq[String] = Seq("tacotron2-cli")
) extends TextToSpeech {
  override val name: String = "tacotron2-cli"

  override def synthesize(text: String, options: TTSOptions): Result[GeneratedAudio] =
    try {
      val tmpOut      = Files.createTempFile("llm4s-tts-", ".wav")
      val baseCommand = command ++ Seq("--text", text, "--out", tmpOut.toString)

      val optFlags = List(
        options.voice.map(v => Seq("--voice", v)),
        options.language.map(l => Seq("--lang", l)),
        options.speakingRate.map(r => Seq("--rate", r.toString)),
        options.pitchSemitones.map(p => Seq("--pitch", p.toString)),
        options.volumeGainDb.map(v => Seq("--gain", v.toString))
      ).flatten

      val args = baseCommand ++ optFlags.combineAll

      val _ = args.!

      val bytes = Files.readAllBytes(tmpOut)
      val meta  = AudioMeta(sampleRate = 22050, numChannels = 1, bitDepth = 16)
      Right(GeneratedAudio(bytes, meta, options.outputFormat))
    } catch {
      case e: Exception => Left(LLMError.fromThrowable(e))
    }
}
