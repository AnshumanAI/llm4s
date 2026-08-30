package org.llm4s.speech

import org.llm4s.speech.config._
import org.llm4s.speech.provider._

/**
 * Main entry point for speech functionality.
 * Provides factory methods for creating TTS and ASR clients.
 * 
 * @author AnshumanAI
 */
object Speech {
  
  /**
   * Create a TTS (Text-to-Speech) client for the specified provider
   */
  def ttsClient(provider: SpeechProvider, config: SpeechProviderConfig): TTSClient = {
    provider match {
      case SpeechProvider.OpenAI     => new OpenAISpeechClient(config.asInstanceOf[OpenAISpeechConfig])
      case SpeechProvider.ElevenLabs => new ElevenLabsClient(config.asInstanceOf[ElevenLabsConfig])
      case SpeechProvider.Azure      => new AzureSpeechClient(config.asInstanceOf[AzureSpeechConfig])
      case SpeechProvider.Google     => new GoogleSpeechClient(config.asInstanceOf[GoogleSpeechConfig])
      case _                         => throw new UnsupportedOperationException(s"TTS not supported for provider: $provider")
    }
  }
  
  /**
   * Create an ASR (Automatic Speech Recognition) client for the specified provider
   */
  def asrClient(provider: SpeechProvider, config: SpeechProviderConfig): ASRClient = {
    provider match {
      case SpeechProvider.OpenAI => new OpenAISpeechClient(config.asInstanceOf[OpenAISpeechConfig])
      case SpeechProvider.Azure  => new AzureSpeechClient(config.asInstanceOf[AzureSpeechConfig])
      case SpeechProvider.Google => new GoogleSpeechClient(config.asInstanceOf[GoogleSpeechConfig])
      case _                     => throw new UnsupportedOperationException(s"ASR not supported for provider: $provider")
    }
  }
}