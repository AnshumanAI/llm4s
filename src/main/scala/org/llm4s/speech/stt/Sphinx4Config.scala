package org.llm4s.speech.stt

import org.llm4s.config.EnvLoader

/**
 * Configuration for Sphinx4 speech recognition.
 * Sphinx4 requires acoustic models, language models, and dictionaries.
 */
case class Sphinx4Config(
  acousticModelPath: String,
  languageModelPath: String,
  dictionaryPath: String,
  sampleRate: Int = 16000,
  numChannels: Int = 1,
  bitDepth: Int = 16
) {
  def validate: Boolean = {
    import java.nio.file.Files
    Files.exists(java.nio.file.Paths.get(acousticModelPath)) &&
    Files.exists(java.nio.file.Paths.get(languageModelPath)) &&
    Files.exists(java.nio.file.Paths.get(dictionaryPath))
  }
}

object Sphinx4Config {

  /**
   * Create Sphinx4Config from environment variables.
   * Common environment variables for Sphinx4 model paths.
   */
  def fromEnv: Option[Sphinx4Config] = {
    val readEnv = EnvLoader.get _

    for {
      acoustic <- readEnv("SPHINX4_ACOUSTIC_MODEL")
      language <- readEnv("SPHINX4_LANGUAGE_MODEL")
      dict     <- readEnv("SPHINX4_DICTIONARY")
    } yield Sphinx4Config(
      acousticModelPath = acoustic,
      languageModelPath = language,
      dictionaryPath = dict
    )
  }

  /**
   * Default English models (if available in standard locations)
   */
  def defaultEnglish: Option[Sphinx4Config] = {
    val home = System.getProperty("user.home")
    val defaultPaths = Seq(
      s"$home/.sphinx4/models/en-us",
      "/usr/share/sphinx4/models/en-us",
      "./models/en-us"
    )

    defaultPaths
      .find { base =>
        val config = Sphinx4Config(
          acousticModelPath = s"$base/acoustic-model",
          languageModelPath = s"$base/language-model.lm.bin",
          dictionaryPath = s"$base/dictionary.dict"
        )
        config.validate
      }
      .map { base =>
        Sphinx4Config(
          acousticModelPath = s"$base/acoustic-model",
          languageModelPath = s"$base/language-model.lm.bin",
          dictionaryPath = s"$base/dictionary.dict"
        )
      }
  }
}


