package org.llm4s.speech.stt

import org.llm4s.speech.AudioInput
import org.llm4s.speech.util.Result
import org.llm4s.llmconnect.error.LLMError
import org.vosk.Model
import org.vosk.Recognizer
import java.io.ByteArrayInputStream

/**
 * Vosk-based speech-to-text implementation.
 * Replaces Sphinx4 as it's more actively maintained and has better performance.
 */
final class VoskSpeechToText(
  modelPath: Option[String] = None
) extends SpeechToText {
  
  override val name: String = "vosk"
  
  override def transcribe(input: AudioInput, options: STTOptions): Result[Transcription] = {
    try {
      // Use default English model if no path provided
      val model = new Model(modelPath.getOrElse("models/vosk-model-small-en-us-0.15"))
      val recognizer = new Recognizer(model, 16000.0f) // Vosk expects 16kHz
      
      // Prepare audio for Vosk (16kHz mono PCM)
      val preparedAudio = prepareAudioForVosk(input)
      
      // Process audio in chunks
      val chunkSize = 4096
      val audioStream = new ByteArrayInputStream(preparedAudio)
      val buffer = new Array[Byte](chunkSize)
      
      var finalResult = ""
      var confidence = 0.0
      var wordTimestamps = List.empty[WordTimestamp]
      
      var bytesRead = audioStream.read(buffer)
      while (bytesRead > 0) {
        if (recognizer.acceptWaveForm(buffer, bytesRead)) {
          val result = recognizer.getResult()
          // Parse JSON result to extract text and confidence
          // This is a simplified implementation
          finalResult += result
        }
        bytesRead = audioStream.read(buffer)
      }
      
      // Get final result
      val finalPartial = recognizer.getFinalResult()
      finalResult += finalPartial
      
      audioStream.close()
      recognizer.close()
      model.close()
      
      Right(Transcription(
        text = finalResult.trim,
        confidence = Some(confidence),
        wordTimestamps = wordTimestamps,
        language = options.language.getOrElse("en")
      ))
      
    } catch {
      case e: Exception => 
        Left(LLMError.fromThrowable(e))
    }
  }
  
  private def prepareAudioForVosk(input: AudioInput): Array[Byte] = {
    // Convert audio to 16kHz mono PCM format expected by Vosk
    // This would integrate with our AudioPreprocessing utilities
    input.audioData // Simplified - would need proper conversion
  }
}
