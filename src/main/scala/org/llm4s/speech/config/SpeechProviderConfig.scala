package org.llm4s.speech.config

/**
 * Base configuration trait for speech providers
 * 
 * @author AnshumanAI
 */
sealed trait SpeechProviderConfig

object SpeechProviderConfig {
  def readEnv(key: String): Option[String] = Option(System.getenv(key))
}

case class OpenAISpeechConfig(
  apiKey: String,
  model: String = "tts-1",
  baseUrl: String = "https://api.openai.com/v1"
) extends SpeechProviderConfig

object OpenAISpeechConfig {
  def fromEnv(modelName: String): OpenAISpeechConfig = {
    val readEnv = SpeechProviderConfig.readEnv _
    OpenAISpeechConfig(
      apiKey = readEnv("OPENAI_API_KEY").getOrElse(
        throw new IllegalArgumentException("OPENAI_API_KEY not set, required when using openai/ model.")
      ),
      model = modelName,
      baseUrl = readEnv("OPENAI_BASE_URL").getOrElse("https://api.openai.com/v1")
    )
  }
}

case class ElevenLabsConfig(
  apiKey: String,
  model: String = "eleven_monolingual_v1",
  baseUrl: String = "https://api.elevenlabs.io/v1"
) extends SpeechProviderConfig

object ElevenLabsConfig {
  def fromEnv(modelName: String): ElevenLabsConfig = {
    val readEnv = SpeechProviderConfig.readEnv _
    ElevenLabsConfig(
      apiKey = readEnv("ELEVENLABS_API_KEY").getOrElse(
        throw new IllegalArgumentException("ELEVENLABS_API_KEY not set, required when using elevenlabs/ model.")
      ),
      model = modelName,
      baseUrl = readEnv("ELEVENLABS_BASE_URL").getOrElse("https://api.elevenlabs.io/v1")
    )
  }
}

case class AzureSpeechConfig(
  apiKey: String,
  region: String,
  model: String = "en-US-JennyNeural",
  baseUrl: String = "https://%s.tts.speech.microsoft.com/cognitiveservices/v1"
) extends SpeechProviderConfig

case class GoogleSpeechConfig(
  apiKey: String,
  model: String = "latest",
  baseUrl: String = "https://texttospeech.googleapis.com/v1"
) extends SpeechProviderConfig