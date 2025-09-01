package org.llm4s.speech.io

import org.llm4s.speech.GeneratedAudio
import org.llm4s.error.LLMError
import org.llm4s.types.Result

import java.io.{ ByteArrayInputStream, FileOutputStream }
import java.nio.file.Path
import javax.sound.sampled.{ AudioFileFormat, AudioFormat => JAudioFormat, AudioInputStream, AudioSystem }

object AudioIO {

  sealed trait AudioIOError extends LLMError
  final case class SaveFailed(message: String, override val context: Map[String, String] = Map.empty)
      extends AudioIOError

  /** Save PCM16 WAV bytes to a file. */
  def saveWav(audio: GeneratedAudio, path: Path): Result[Path] =
    try {
      val format = new JAudioFormat(
        audio.meta.sampleRate.toFloat,
        audio.meta.bitDepth,
        audio.meta.numChannels,
        /* signed = */ true,
        /* bigEndian = */ false
      )
      val bais = new ByteArrayInputStream(audio.data)
      val ais  = new AudioInputStream(bais, format, audio.data.length / format.getFrameSize)
      val _    = AudioSystem.write(ais, AudioFileFormat.Type.WAVE, path.toFile)
      Right(path)
    } catch {
      case e: Exception => Left(SaveFailed(Option(e.getMessage).getOrElse("Failed to save WAV")))
    }

  /** Save raw PCM16 little-endian to a file. */
  def saveRawPcm16(audio: GeneratedAudio, path: Path): Result[Path] =
    try {
      val fos = new FileOutputStream(path.toFile)
      try fos.write(audio.data)
      finally fos.close()
      Right(path)
    } catch {
      case e: Exception => Left(SaveFailed(Option(e.getMessage).getOrElse("Failed to save raw PCM")))
    }
}
