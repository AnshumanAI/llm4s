package org.llm4s

import org.llm4s.speech.model._

/**
 * Speech processing package containing TTS and ASR functionality.
 * 
 * @author AnshumanAI
 */
package object speech {
  
  /**
   * Trait for Text-to-Speech clients
   */
  trait TTSClient {
    def synthesize(text: String, options: TTSSynthesisOptions): Either[SpeechError, AudioResponse]
  }
  
  /**
   * Trait for Automatic Speech Recognition clients
   */
  trait ASRClient {
    def transcribe(audioData: Array[Byte], options: ASRTranscriptionOptions): Either[SpeechError, TranscriptionResponse]
  }
}