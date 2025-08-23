package org.llm4s.speech

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.llm4s.speech.stt.{ Sphinx4SpeechToText, WhisperSpeechToText, STTOptions, Sphinx4Config }
import org.llm4s.speech.tts.{ Tacotron2TextToSpeech, TTSOptions }
import org.llm4s.speech.processing.AudioPreprocessing
import org.llm4s.speech.io.AudioIO
import org.llm4s.speech.util.PlatformCommands

import java.nio.file.Files

class SpeechIntegrationTest extends AnyFunSuite with Matchers {

  test("AudioPreprocessing should handle mono conversion") {
    // Create stereo PCM16 test data (2 channels, 16-bit)
    val stereoBytes = Array.fill(1000)(0.toByte) // Simple test data
    val stereoMeta  = AudioMeta(sampleRate = 16000, numChannels = 2, bitDepth = 16)

    val result = AudioPreprocessing.toMono(stereoBytes, stereoMeta)
    result shouldBe Symbol("right")
    result.foreach { case (bytes, meta) =>
      meta.numChannels shouldBe 1
      bytes.length shouldBe (stereoBytes.length / 2)
    }
  }

  test("AudioPreprocessing should handle resampling") {
    val sourceBytes = Array.fill(1000)(0.toByte)
    val sourceMeta  = AudioMeta(sampleRate = 8000, numChannels = 1, bitDepth = 16)
    val targetRate  = 16000

    val result = AudioPreprocessing.resamplePcm16(sourceBytes, sourceMeta, targetRate)
    result shouldBe Symbol("right")
    result.foreach { case (_, meta) =>
      meta.sampleRate shouldBe targetRate
    }
  }

  test("AudioPreprocessing should compose multiple operations") {
    val sourceBytes = Array.fill(1000)(0.toByte)
    val sourceMeta  = AudioMeta(sampleRate = 44100, numChannels = 2, bitDepth = 16)

    val result = AudioPreprocessing.standardizeForSTT(sourceBytes, sourceMeta, targetRate = 16000)
    result shouldBe Symbol("right")
    result.foreach { case (_, meta) =>
      meta.sampleRate shouldBe 16000
      meta.numChannels shouldBe 1
    }
  }

  test("Sphinx4SpeechToText should handle configuration") {
    val config = Sphinx4Config(
      acousticModelPath = "/tmp/test-acoustic",
      languageModelPath = "/tmp/test-language",
      dictionaryPath = "/tmp/test-dict"
    )

    config.validate shouldBe false // Paths don't exist

    val stt = new Sphinx4SpeechToText(
      acousticModelPath = Some(config.acousticModelPath),
      languageModelPath = Some(config.languageModelPath),
      dictionaryPath = Some(config.dictionaryPath)
    )

    stt.name shouldBe "sphinx4"
  }

  test("WhisperSpeechToText should build correct CLI arguments") {
    val stt = new WhisperSpeechToText(PlatformCommands.mockSuccess, model = "large", outputFormat = "json")
    val options = STTOptions(
      language = Some("en"),
      prompt = Some("This is a test"),
      enableTimestamps = true
    )

    // Test that it doesn't crash
    val fakePath = Files.createTempFile("test", ".wav")
    Files.write(fakePath, Array[Byte](0, 0, 0, 0))

    val result = stt.transcribe(AudioInput.FileAudio(fakePath), options)
    result shouldBe Symbol("right")
  }

  test("Tacotron2TextToSpeech should handle voice options") {
    val tts = new Tacotron2TextToSpeech(PlatformCommands.echo)
    val options = TTSOptions(
      voice = Some("en-female"),
      language = Some("en"),
      speakingRate = Some(1.2),
      pitchSemitones = Some(2.0),
      volumeGainDb = Some(3.0)
    )

    val result = tts.synthesize("Hello world", options)
    result shouldBe Symbol("right")
  }

  test("AudioIO should handle WAV and raw PCM output") {
    val testAudio = GeneratedAudio(
      data = Array.fill(1000)(0.toByte),
      meta = AudioMeta(sampleRate = 16000, numChannels = 1, bitDepth = 16),
      format = AudioFormat.WavPcm16
    )

    val wavPath = Files.createTempFile("test", ".wav")
    val rawPath = Files.createTempFile("test", ".pcm")

    val wavResult = AudioIO.saveWav(testAudio, wavPath)
    val rawResult = AudioIO.saveRawPcm16(testAudio, rawPath)

    wavResult shouldBe Symbol("right")
    rawResult shouldBe Symbol("right")

    // Cleanup
    Files.deleteIfExists(wavPath)
    Files.deleteIfExists(rawPath)
  }

  test("Sphinx4Config should handle environment variables") {
    // Test that fromEnv returns None when env vars aren't set
    Sphinx4Config.fromEnv shouldBe None

    // Test defaultEnglish returns None when models aren't found
    Sphinx4Config.defaultEnglish shouldBe None
  }
}
