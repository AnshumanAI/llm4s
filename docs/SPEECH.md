# Speech TTS and ASR Functionality

This document describes the Text-to-Speech (TTS) and Automatic Speech Recognition (ASR) functionality implemented in LLM4S as part of the GSoC proposal.

## Overview

The speech functionality provides a unified interface for:
- **Text-to-Speech (TTS)**: Convert text to spoken audio
- **Automatic Speech Recognition (ASR)**: Convert spoken audio to text

## Supported Providers

### TTS Providers
- **OpenAI**: High-quality TTS with multiple voices
- **Azure Speech**: Microsoft's speech synthesis service
- **Google Speech**: Google's text-to-speech API
- **ElevenLabs**: Specialized in voice cloning and natural speech

### ASR Providers
- **OpenAI Whisper**: High-accuracy speech recognition
- **Azure Speech**: Microsoft's speech-to-text service
- **Google Speech**: Google's speech recognition API

## Quick Start

### Basic TTS Usage

```scala
import org.llm4s.speech._
import org.llm4s.speech.model._
import org.llm4s.speech.provider._

// Using environment variables
val ttsClient = Speech.ttsClient()
val result = ttsClient.synthesize(
  "Hello, this is a test of text-to-speech functionality.",
  TTSSynthesisOptions(voice = "alloy", speed = 1.0)
)

result match {
  case Right(audioResponse) =>
    println(s"Generated ${audioResponse.audioData.length} bytes of audio")
  case Left(error) =>
    println(s"Error: ${error.message}")
}
```

### Basic ASR Usage

```scala
import org.llm4s.speech._
import org.llm4s.speech.model._
import org.llm4s.speech.provider._

// Using environment variables
val asrClient = Speech.asrClient()
val audioData = // ... your audio data as Array[Byte]
val result = asrClient.transcribe(
  audioData,
  ASRTranscriptionOptions(language = Some("en"))
)

result match {
  case Right(transcription) =>
    println(s"Transcribed: ${transcription.text}")
  case Left(error) =>
    println(s"Error: ${error.message}")
}
```

## Configuration

### Environment Variables

Set the following environment variables based on your chosen provider:

#### OpenAI
```bash
export SPEECH_MODEL="openai/tts-1"  # or "openai/whisper-1" for ASR
export OPENAI_API_KEY="your-openai-api-key"
export OPENAI_BASE_URL="https://api.openai.com/v1"  # optional
```

#### Azure Speech
```bash
export SPEECH_MODEL="azure/en-US-JennyNeural"
export AZURE_SPEECH_API_KEY="your-azure-speech-key"
export AZURE_SPEECH_REGION="your-azure-region"
```

#### Google Speech
```bash
export SPEECH_MODEL="google/latest"
export GOOGLE_SPEECH_API_KEY="your-google-api-key"
```

#### ElevenLabs (TTS only)
```bash
export SPEECH_MODEL="elevenlabs/eleven_monolingual_v1"
export ELEVENLABS_API_KEY="your-elevenlabs-api-key"
```

### Explicit Configuration

```scala
import org.llm4s.speech.config._
import org.llm4s.speech.provider._

// OpenAI configuration
val openAIConfig = OpenAISpeechConfig(
  apiKey = "your-api-key",
  model = "tts-1",
  baseUrl = "https://api.openai.com/v1"
)

val ttsClient = Speech.ttsClient(SpeechProvider.OpenAI, openAIConfig)
```

## API Reference

### Speech Object

The main entry point for speech functionality:

```scala
object Speech {
  // Factory methods
  def ttsClient(): TTSClient
  def asrClient(): ASRClient
  def ttsClient(provider: SpeechProvider, config: SpeechProviderConfig): TTSClient
  def asrClient(provider: SpeechProvider, config: SpeechProviderConfig): ASRClient
  
  // Convenience methods
  def synthesize(text: String, options: TTSSynthesisOptions = TTSSynthesisOptions()): Either[SpeechError, AudioResponse]
  def transcribe(audioData: Array[Byte], options: ASRTranscriptionOptions = ASRTranscriptionOptions()): Either[SpeechError, TranscriptionResponse]
}
```

### TTS Options

```scala
case class TTSSynthesisOptions(
  voice: String = "alloy",
  model: String = "tts-1",
  responseFormat: String = "mp3",
  speed: Double = 1.0,
  temperature: Double = 1.0
)
```

### ASR Options

```scala
case class ASRTranscriptionOptions(
  model: String = "whisper-1",
  language: Option[String] = None,
  prompt: Option[String] = None,
  responseFormat: String = "json",
  temperature: Double = 0.0,
  timestampGranularities: Seq[String] = Seq("word", "segment")
)
```

### Response Models

#### AudioResponse
```scala
case class AudioResponse(
  audioData: Array[Byte],
  format: String,
  duration: Option[Double] = None,
  wordCount: Option[Int] = None
)
```

#### TranscriptionResponse
```scala
case class TranscriptionResponse(
  text: String,
  language: Option[String] = None,
  duration: Option[Double] = None,
  segments: Seq[TranscriptionSegment] = Seq.empty
)
```

## Error Handling

The speech API uses `Either[SpeechError, Response]` for error handling:

```scala
sealed trait SpeechError extends Exception
case class SpeechAuthenticationError(message: String) extends SpeechError
case class SpeechRateLimitError(message: String) extends SpeechError
case class SpeechValidationError(message: String) extends SpeechError
case class SpeechUnknownError(cause: Throwable) extends SpeechError
```

## Examples

### Voice Assistant Integration

```scala
import org.llm4s.llmconnect.LLM
import org.llm4s.llmconnect.model._
import org.llm4s.speech._

// Create a voice assistant that:
// 1. Transcribes user speech
// 2. Processes with LLM
// 3. Converts response to speech

val asrClient = Speech.asrClient()
val ttsClient = Speech.ttsClient()
val llmClient = LLM.client()

// Transcribe audio
val audioData = // ... audio input
val transcription = asrClient.transcribe(audioData)

transcription.foreach { result =>
  result match {
    case Right(transcription) =>
      // Process with LLM
      val llmResponse = llmClient.complete(
        Conversation(Seq(UserMessage(transcription.text)))
      )
      
      llmResponse.foreach { completion =>
        // Convert LLM response to speech
        val ttsResponse = ttsClient.synthesize(completion.message.content)
        ttsResponse.foreach { audioResponse =>
          // Play or save the audio
          saveAudioToFile(audioResponse.audioData, "response.mp3")
        }
      }
      
    case Left(error) =>
      println(s"Transcription failed: ${error.message}")
  }
}
```

### Batch Processing

```scala
import org.llm4s.speech._

// Process multiple text inputs
val texts = Seq(
  "Hello, how are you?",
  "The weather is nice today.",
  "Thank you for your help."
)

val ttsClient = Speech.ttsClient()

texts.zipWithIndex.foreach { case (text, index) =>
  ttsClient.synthesize(text).foreach { audioResponse =>
    saveAudioToFile(audioResponse.audioData, s"output_$index.mp3")
  }
}
```

## Integration with Existing LLM4S Features

The speech functionality integrates seamlessly with existing LLM4S features:

- **LLM Integration**: Use transcribed text as input to LLM conversations
- **Tool API**: Create speech-related tools for agents
- **Tracing**: Speech operations can be traced like other LLM operations
- **Workspace Integration**: Save and load audio files in workspace

## Performance Considerations

- **Audio Format**: Use MP3 for good compression/quality balance
- **Batch Processing**: Process multiple requests together when possible
- **Caching**: Cache frequently used TTS outputs
- **Streaming**: Consider streaming for real-time applications

## Troubleshooting

### Common Issues

1. **Authentication Errors**: Check API keys and permissions
2. **Rate Limiting**: Implement exponential backoff for retries
3. **Audio Format**: Ensure audio is in supported format (MP3, WAV, etc.)
4. **File Size**: Check provider limits for audio file sizes

### Debug Mode

Enable debug logging to see detailed API requests:

```scala
// Set log level to DEBUG in your logging configuration
```

## Future Enhancements

- **Streaming TTS**: Real-time audio generation
- **Voice Cloning**: Custom voice training
- **Multi-language Support**: Enhanced language detection
- **Audio Processing**: Noise reduction, audio enhancement
- **Real-time ASR**: Streaming speech recognition