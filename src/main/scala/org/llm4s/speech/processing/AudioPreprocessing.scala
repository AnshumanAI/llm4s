package org.llm4s.speech.processing

import org.llm4s.error.LLMError
import org.llm4s.types.Result
import org.llm4s.speech.{ AudioMeta, AudioFormat, GeneratedAudio }

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import javax.sound.sampled.{ AudioFormat => JAudioFormat, AudioInputStream, AudioSystem }

/**
 * Functional audio preprocessing utilities.
 * These are pure transformations described as functions that return either errors or processed audio.
 */
object AudioPreprocessing {

  sealed trait AudioProcError extends LLMError
  final case class OperationFailed(message: String, override val context: Map[String, String] = Map.empty)
      extends AudioProcError

  /** Resample PCM16 little-endian bytes to target sample rate using Java Sound. */
  def resamplePcm16(bytes: Array[Byte], source: AudioMeta, targetRate: Int): Result[(Array[Byte], AudioMeta)] =
    try {
      val srcFormat = new JAudioFormat(
        source.sampleRate.toFloat,
        source.bitDepth,
        source.numChannels,
        /* signed = */ true,
        /* bigEndian = */ false
      )
      val srcAis =
        new AudioInputStream(new ByteArrayInputStream(bytes), srcFormat, bytes.length / srcFormat.getFrameSize)
      val dstFormat = new JAudioFormat(
        targetRate.toFloat,
        source.bitDepth,
        source.numChannels,
        /* signed = */ true,
        /* bigEndian = */ false
      )
      val converted = AudioSystem.getAudioInputStream(dstFormat, srcAis)
      val out       = new ByteArrayOutputStream(bytes.length)
      val buf       = new Array[Byte](8192)
      var read      = 0
      while ({ read = converted.read(buf); read } != -1) out.write(buf, 0, read)
      Right(out.toByteArray -> source.copy(sampleRate = targetRate))
    } catch {
      case e: Exception => Left(OperationFailed(Option(e.getMessage).getOrElse("Resample failed")))
    }

  /** Convert to mono by averaging channels (PCM16 little-endian). */
  def toMono(bytes: Array[Byte], meta: AudioMeta): Result[(Array[Byte], AudioMeta)] =
    if (meta.numChannels <= 1) Right((bytes, meta))
    else
      try {
        val frameSize     = (meta.bitDepth / 8) * meta.numChannels
        val numFrames     = bytes.length / frameSize
        val monoFrameSize = meta.bitDepth / 8
        val out           = new Array[Byte](numFrames * monoFrameSize)
        
        (0 until numFrames).foreach { frameIndex =>
          val sum = (0 until meta.numChannels).foldLeft(0) { (acc, ch) =>
            val base = frameIndex * frameSize + ch * (meta.bitDepth / 8)
            val sample = ((bytes(base + 1) << 8) | (bytes(base) & 0xff)).toShort.toInt
            acc + sample
          }
          
          val avg: Short = (sum / meta.numChannels).toShort
          val outByteIndex = frameIndex * 2
          out(outByteIndex) = (avg & 0xff).toByte
          out(outByteIndex + 1) = ((avg >> 8) & 0xff).toByte
        }
        
        Right(out -> meta.copy(numChannels = 1))
      } catch {
        case e: Exception => Left(OperationFailed(Option(e.getMessage).getOrElse("Mono mix failed")))
      }

  /** Trim leading and trailing silence using a simple amplitude threshold on PCM16. */
  def trimSilence(bytes: Array[Byte], meta: AudioMeta, threshold: Int = 512): Result[(Array[Byte], AudioMeta)] =
    try {
      val sampleSize = meta.bitDepth / 8
      val frameSize  = sampleSize * meta.numChannels
      val numFrames  = bytes.length / frameSize
      def frameLoud(frameIdx: Int): Boolean = {
        val maxAmplitude = (0 until meta.numChannels).foldLeft(0) { (max, ch) =>
          val base = frameIdx * frameSize + ch * sampleSize
          val sample = ((bytes(base + 1) << 8) | (bytes(base) & 0xff)).toShort
          val amplitude = math.abs(sample.toInt)
          math.max(max, amplitude)
        }
        maxAmplitude >= threshold
      }
      var start = 0
      while (start < numFrames && !frameLoud(start)) start += 1
      var end = numFrames - 1
      while (end >= start && !frameLoud(end)) end -= 1
      val outStart = start * frameSize
      val outEnd   = (end + 1) * frameSize
      val sliced =
        if (outEnd > outStart) java.util.Arrays.copyOfRange(bytes, outStart, outEnd) else Array.emptyByteArray
      Right(sliced -> meta)
    } catch {
      case e: Exception => Left(OperationFailed(Option(e.getMessage).getOrElse("Trim failed")))
    }

  /** Compose multiple steps functionally */
  def standardizeForSTT(
    bytes: Array[Byte],
    meta: AudioMeta,
    targetRate: Int = 16000
  ): Result[(Array[Byte], AudioMeta)] =
    for {
      mono       <- toMono(bytes, meta)
      resampled  <- resamplePcm16(mono._1, mono._2, targetRate)
      normalized <- trimSilence(resampled._1, resampled._2)
    } yield normalized

  def wrap(bytes: Array[Byte], meta: AudioMeta, format: AudioFormat = AudioFormat.WavPcm16): GeneratedAudio =
    GeneratedAudio(bytes, meta, format)
}
nire 