package org.llm4s.speech.processing

import org.llm4s.speech.AudioMeta
import org.llm4s.types.Result

/**
 * Generic audio validator trait for validating audio data and metadata.
 * This provides a more flexible and extensible design for audio validation.
 */
trait AudioValidator[A] {
  def validate(input: A): Result[A]
  def name: String
}

/**
 * Audio validation implementations
 */
object AudioValidator {

  /**
   * Validates audio metadata for STT processing
   */
  case class STTMetadataValidator() extends AudioValidator[AudioMeta] {
    def validate(meta: AudioMeta): Result[AudioMeta] = {
      val errors = List(
        if (meta.sampleRate <= 0) Some("Sample rate must be positive") else None,
        if (meta.numChannels <= 0) Some("Number of channels must be positive") else None,
        if (meta.bitDepth != 16) Some("Only 16-bit audio is supported") else None,
        if (meta.sampleRate > 48000) Some("Sample rate too high for STT") else None
      ).flatten

      if (errors.isEmpty) {
        Right(meta)
      } else {
        Left(AudioPreprocessing.OperationFailed(s"Validation failed: ${errors.mkString(", ")}"))
      }
    }

    def name: String = "stt-metadata-validator"
  }

  /**
   * Validates audio data length matches metadata
   */
  case class AudioDataValidator() extends AudioValidator[(Array[Byte], AudioMeta)] {
    def validate(input: (Array[Byte], AudioMeta)): Result[(Array[Byte], AudioMeta)] = {
      val (bytes, meta)  = input
      val expectedLength = meta.numChannels * (meta.bitDepth / 8)

      if (bytes.length % expectedLength != 0) {
        Left(
          AudioPreprocessing.OperationFailed(
            s"Audio data length (${bytes.length}) is not a multiple of frame size (${expectedLength})"
          )
        )
      } else {
        Right(input)
      }
    }

    def name: String = "audio-data-validator"
  }

  /**
   * Validates audio is not empty
   */
  case class NonEmptyAudioValidator() extends AudioValidator[(Array[Byte], AudioMeta)] {
    def validate(input: (Array[Byte], AudioMeta)): Result[(Array[Byte], AudioMeta)] = {
      val (bytes, _) = input
      if (bytes.isEmpty) {
        Left(AudioPreprocessing.OperationFailed("Audio data is empty"))
      } else {
        Right(input)
      }
    }

    def name: String = "non-empty-audio-validator"
  }

  /**
   * Composes multiple validators
   */
  case class CompositeValidator[A](
    validators: List[AudioValidator[A]]
  ) extends AudioValidator[A] {

    def validate(input: A): Result[A] =
      validators.foldLeft(Right(input): Result[A])((acc, validator) => acc.flatMap(validator.validate))

    def name: String = validators.map(_.name).mkString(" + ")
  }

  /**
   * Standard STT validation pipeline
   */
  def sttValidator: AudioValidator[(Array[Byte], AudioMeta)] =
    CompositeValidator(
      List(
        NonEmptyAudioValidator(),
        AudioDataValidator()
      )
    )
}
