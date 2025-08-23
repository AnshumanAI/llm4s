package org.llm4s.speech.tts

import org.llm4s.error.LLMError
import org.llm4s.types.Result
import org.llm4s.speech.{ GeneratedAudio, AudioMeta }

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
      val tmpOut = Files.createTempFile("llm4s-tts-", ".wav")
      val args =
        command ++ Seq(
          "--text",
          text,
          "--out",
          tmpOut.toString
        ) ++ options.voice.map(v => Seq("--voice", v)).getOrElse(Seq.empty) ++
          options.language.map(l => Seq("--lang", l)).getOrElse(Seq.empty) ++
          options.speakingRate.map(r => Seq("--rate", r.toString)).getOrElse(Seq.empty) ++
          options.pitchSemitones.map(p => Seq("--pitch", p.toString)).getOrElse(Seq.empty) ++
          options.volumeGainDb.map(v => Seq("--gain", v.toString)).getOrElse(Seq.empty)

      val _ = args.!

      val bytes = Files.readAllBytes(tmpOut)
      val meta  = AudioMeta(sampleRate = 22050, numChannels = 1, bitDepth = 16)
      Right(GeneratedAudio(bytes, meta, options.outputFormat))
    } catch {
      case e: Exception => Left(LLMError.fromThrowable(e))
    }
}
