# LLM4S Speech TTS and ASR Implementation

This document describes the implementation of Text-to-Speech (TTS) and Automatic Speech Recognition (ASR) functionality in LLM4S as part of the GSoC proposal.

## 🎯 Overview

The speech functionality provides a unified interface for:
- **Text-to-Speech (TTS)**: Convert text to spoken audio
- **Automatic Speech Recognition (ASR)**: Convert spoken audio to text

## 🏗️ Architecture

The implementation follows the same patterns as the existing LLM4S codebase:

```
org.llm4s.speech/
├── Speech.scala                    # Main entry point
├── SpeechConnect.scala             # Client factory
├── TTSClient.scala                 # TTS interface
├── ASRClient.scala                 # ASR interface
├── config/
│   └── SpeechProviderConfig.scala  # Configuration classes
├── model/
│   ├── SpeechError.scala           # Error handling
│   ├── TTSSynthesisOptions.scala   # TTS options
│   └── ASRTranscriptionOptions.scala # ASR options
└── provider/
    ├── SpeechProvider.scala        # Provider enumeration
    ├── OpenAISpeechClient.scala    # OpenAI implementation
    ├── AzureSpeechClient.scala     # Azure implementation
    ├── GoogleSpeechClient.scala    # Google implementation
    └── ElevenLabsClient.scala      # ElevenLabs implementation
```

## 🚀 Quick Start

### Basic TTS Usage

```scala
import org.llm4s.speech._
import org.llm4s.speech.model._

// Using environment variables
val ttsClient = Speech.ttsClient()
val result = ttsClient.synthesize(
  "Hello, this is a test of text-to-speech functionality.",
  TTSSynthesisOptions(voice = "alloy", speed = 1.0)
)

result match {
  case Right(audioResponse) =>
    println(s"Generated ${audioResponse.audioData.length} bytes of audio")
    // Save to file
    java.nio.file.Files.write(
      java.nio.file.Path.of("output.mp3"), 
      audioResponse.audioData
    )
  case Left(error) =>
    println(s"Error: ${error.message}")
}
```

### Basic ASR Usage

```scala
import org.llm4s.speech._
import org.llm4s.speech.model._

// Using environment variables
val asrClient = Speech.asrClient()
val audioData = java.nio.file.Files.readAllBytes(
  java.nio.file.Path.of("input.mp3")
)

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

## 🔧 Configuration

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

## 📋 API Reference

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

## 🎭 Voice Assistant Example

Here's a complete example of a voice assistant that integrates TTS, ASR, and LLM:

```scala
import org.llm4s.llmconnect.LLM
import org.llm4s.llmconnect.model._
import org.llm4s.speech._

object VoiceAssistant {
  def main(args: Array[String]): Unit = {
    // Initialize clients
    val ttsClient = Speech.ttsClient()
    val asrClient = Speech.asrClient()
    val llmClient = LLM.client()

    // System prompt
    val systemPrompt = SystemMessage(
      "You are a helpful voice assistant. Keep responses concise and natural for speech."
    )

    // Main conversation loop
    var conversation = Conversation(Seq(systemPrompt))
    
    while (true) {
      // Get user input (simulated here)
      val userInput = scala.io.StdIn.readLine("You: ")
      if (userInput == "quit") return

      // Add user message
      val userMessage = UserMessage(userInput)
      conversation = conversation.addMessage(userMessage)

      // Get LLM response
      llmClient.complete(conversation, CompletionOptions()) match {
        case Right(completion) =>
          val responseText = completion.message.content
          println(s"Assistant: $responseText")

          // Convert to speech
          ttsClient.synthesize(responseText) match {
            case Right(audioResponse) =>
              // Save audio response
              java.nio.file.Files.write(
                java.nio.file.Path.of("response.mp3"), 
                audioResponse.audioData
              )
              println("Audio saved to response.mp3")
              
            case Left(error) =>
              println(s"TTS failed: ${error.message}")
          }

        case Left(error) =>
          println(s"LLM failed: ${error.message}")
      }
    }
  }
}
```

## 🔄 Integration with Existing Features

The speech functionality integrates seamlessly with existing LLM4S features:

### LLM Integration
```scala
// Use transcribed text as LLM input
val transcription = asrClient.transcribe(audioData)
transcription.foreach { result =>
  result.foreach { transcription =>
    val llmResponse = llmClient.complete(
      Conversation(Seq(UserMessage(transcription.text)))
    )
    // Process LLM response...
  }
}
```

### Tool API Integration
```scala
// Create speech-related tools for agents
import org.llm4s.toolapi._

case class TTSTool() extends Tool[String, AudioResponse] {
  def name = "text_to_speech"
  def description = "Convert text to speech"
  def schema = ObjectSchema[String]()
  
  def execute(text: String): Either[String, AudioResponse] = {
    Speech.synthesize(text).left.map(_.message)
  }
}
```

### Workspace Integration
```scala
// Save and load audio files in workspace
import org.llm4s.workspace._

val workspace = ContainerisedWorkspace("/workspace")

// Save TTS output
val audioResponse = ttsClient.synthesize("Hello world")
audioResponse.foreach { response =>
  workspace.writeFile("output.mp3", response.audioData)
}

// Load audio for ASR
val audioData = workspace.readFile("input.mp3")
val transcription = asrClient.transcribe(audioData)
```

## 🧪 Testing

Run the speech tests:

```bash
sbt "testOnly org.llm4s.speech.SpeechTest"
```

## 📚 Examples

See the examples in `samples/src/main/scala/org/llm4s/samples/speech/`:

- `SpeechExample.scala`: Basic TTS and ASR usage
- `VoiceAssistantExample.scala`: Complete voice assistant implementation

## 🚨 Error Handling

The speech API uses `Either[SpeechError, Response]` for error handling:

```scala
sealed trait SpeechError extends Exception
case class SpeechAuthenticationError(message: String) extends SpeechError
case class SpeechRateLimitError(message: String) extends SpeechError
case class SpeechValidationError(message: String) extends SpeechError
case class SpeechUnknownError(cause: Throwable) extends SpeechError
```

## 🔧 Troubleshooting

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

## 🎯 GSoC Implementation Status

This implementation fulfills the GSoC proposal requirements:

- ✅ **TTS Support**: Multiple providers (OpenAI, Azure, Google, ElevenLabs)
- ✅ **ASR Support**: Multiple providers (OpenAI, Azure, Google)
- ✅ **Unified API**: Consistent interface across providers
- ✅ **Error Handling**: Comprehensive error types and handling
- ✅ **Integration**: Seamless integration with existing LLM4S features
- ✅ **Documentation**: Comprehensive documentation and examples
- ✅ **Testing**: Unit tests for all components

## 🔮 Future Enhancements

- **Streaming TTS**: Real-time audio generation
- **Voice Cloning**: Custom voice training
- **Multi-language Support**: Enhanced language detection
- **Audio Processing**: Noise reduction, audio enhancement
- **Real-time ASR**: Streaming speech recognition
- **Voice Activity Detection**: Automatic speech detection
- **Speaker Identification**: Multi-speaker support

## 📖 Additional Resources

- [Speech Documentation](docs/SPEECH.md)
- [API Reference](docs/API.md)
- [Examples](samples/src/main/scala/org/llm4s/samples/speech/)
- [GSoC Proposal](https://drive.google.com/file/d/1AyIPiuX6b8OVgkC_imQ3ydaWwpI6g1uH/view?usp=drivesdk)