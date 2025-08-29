package org.llm4s.speech.processing

import org.llm4s.speech.AudioMeta
import org.llm4s.types.Result

/**
 * Generic audio converter trait for transforming audio between different formats.
 * This provides a more flexible and extensible design for audio processing.
 */
trait AudioConverter[From, To] {
  def convert(input: From): Result[To]
  def name: String
}

/**
 * Audio format converter implementations
 */
object AudioConverter {

  /**
   * Converts audio bytes to mono format
   */
  case class MonoConverter() extends AudioConverter[(Array[Byte], AudioMeta), (Array[Byte], AudioMeta)] {
    def convert(input: (Array[Byte], AudioMeta)): Result[(Array[Byte], AudioMeta)] = {
      val (bytes, meta) = input
      AudioPreprocessing.toMono(bytes, meta)
    }

    def name: String = "mono-converter"
  }

  /**
   * Converts audio sample rate
   */
  case class ResampleConverter(targetRate: Int)
      extends AudioConverter[(Array[Byte], AudioMeta), (Array[Byte], AudioMeta)] {
    def convert(input: (Array[Byte], AudioMeta)): Result[(Array[Byte], AudioMeta)] = {
      val (bytes, meta) = input
      AudioPreprocessing.resamplePcm16(bytes, meta, targetRate)
    }

    def name: String = s"resample-converter-${targetRate}Hz"
  }

  /**
   * Trims silence from audio
   */
  case class SilenceTrimmer(threshold: Int = 512)
      extends AudioConverter[(Array[Byte], AudioMeta), (Array[Byte], AudioMeta)] {
    def convert(input: (Array[Byte], AudioMeta)): Result[(Array[Byte], AudioMeta)] = {
      val (bytes, meta) = input
      AudioPreprocessing.trimSilence(bytes, meta, threshold)
    }

    def name: String = s"silence-trimmer-${threshold}"
  }

  /**
   * Composes multiple converters in sequence
   */
  case class CompositeConverter[A, B, C](
    first: AudioConverter[A, B],
    second: AudioConverter[B, C]
  ) extends AudioConverter[A, C] {

    def convert(input: A): Result[C] =
      for {
        intermediate <- first.convert(input)
        result       <- second.convert(intermediate)
      } yield result

    def name: String = s"${first.name} -> ${second.name}"
  }

  /**
   * Standard STT preprocessing pipeline
   */
  def sttPreprocessor(targetRate: Int = 16000): AudioConverter[(Array[Byte], AudioMeta), (Array[Byte], AudioMeta)] =
    CompositeConverter(
      MonoConverter(),
      CompositeConverter(
        ResampleConverter(targetRate),
        SilenceTrimmer()
      )
    )
}
