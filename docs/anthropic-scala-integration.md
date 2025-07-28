# Anthropic API Integration Guide for Scala

This guide provides Scala developers with comprehensive information on integrating the Anthropic API in Scala applications. It includes setup instructions, code examples, and best practices for working with Claude models using the Anthropic Java SDK in Scala.

## Table of Contents

1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Basic Usage](#basic-usage)
   - [Synchronous Requests](#synchronous-requests)
   - [Asynchronous Requests](#asynchronous-requests)
   - [Streaming Responses](#streaming-responses)
4. [Working with Tools](#working-with-tools)
5. [Advanced Configuration](#advanced-configuration)
6. [Best Practices](#best-practices)
7. [Error Handling](#error-handling)

## Introduction

The Anthropic Java SDK provides a robust API for integrating Claude models into your applications. When working with Scala, you can leverage this Java SDK with Scala's functional features. The Anthropic API allows you to:

- Create structured API calls with JSON Schema validation
- Safely execute requests to Claude models
- Process responses with Scala's functional programming features
- Integrate with tools for enhanced functionality

## Installation

Add the LLM4S dependencies to your build.sbt:

```scala
libraryDependencies ++= Seq(
  "com.anthropic" % "anthropic-java" % "0.7.0"
)
```

## Basic Usage

### Synchronous Requests

Here's a simple example of sending a synchronous request to Claude:

```scala
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient

object BasicAnthropicExample {
  def main(args: Array[String]): Unit = {
    // Get client connection using environment variables
    val anthropicClient = AnthropicOkHttpClient.fromEnv()
    
    // Create message parameters
    val messageParams = MessageCreateParams.builder()
      .model(Model.CLAUDE_3_7_SONNET_LATEST)
      .maxTokens(1024)
      .addUserMessage("Tell me about functional programming in Scala.")
      .build()
    
    // Send the request and get the response
    val response = anthropicClient.messages().create(messageParams)
    
    // Print the response text
    response.content().stream()
      .flatMap(contentBlock => contentBlock.text().stream())
      .forEach(textBlock => println(textBlock.text()))
  }
}
```

### Asynchronous Requests

For non-blocking operations, use the asynchronous client:

```scala
import com.anthropic.client.AnthropicClientAsync
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object AsyncAnthropicExample {
  def main(args: Array[String]): Unit = {
    // Get async client
    val anthropicClientAsync = AnthropicOkHttpClientAsync.fromEnv()
    
    val messageParams = MessageCreateParams.builder()
      .model(Model.CLAUDE_3_7_SONNET_LATEST)
      .maxTokens(1024)
      .addUserMessage("Explain monads in simple terms.")
      .build()
    
    // Send the request asynchronously
    val responseFuture = anthropicClientAsync.messages().create(messageParams)
    
    // Handle the future response
    responseFuture.foreach { response =>
      response.content().stream()
        .flatMap(contentBlock => contentBlock.text().stream())
        .forEach(textBlock => println(textBlock.text()))
    }
    
    // Keep the main thread alive
    Thread.sleep(5000)
  }
}
```

### Streaming Responses

For longer responses or real-time processing, use streaming:

```scala
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.http.StreamResponse
import com.anthropic.models.RawMessageStreamEvent

object StreamingAnthropicExample {
  def main(args: Array[String]): Unit = {
    val anthropicClient = AnthropicOkHttpClient.fromEnv()
    
    val messageParams = MessageCreateParams.builder()
      .model(Model.CLAUDE_3_7_SONNET_LATEST)
      .maxTokens(2048)
      .addUserMessage("Write a short story about a Scala programmer.")
      .build()
    
    // Create a streaming request
    try (val streamResponse: StreamResponse[RawMessageStreamEvent] = 
         anthropicClient.messages().createStreaming(messageParams)) {
      
      // Process the stream as it arrives
      streamResponse.stream()
        .flatMap(event => event.contentBlockDelta().stream())
        .flatMap(deltaEvent => deltaEvent.delta().text().stream())
        .forEach(textDelta => print(textDelta.text()))
    }
  }
}
```

## Working with Tools

Claude supports tools to extend its capabilities. Here's how to use tools with LLM4S:

```scala
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.{Tool, MessageCreateParams, Model}
import com.anthropic.models.Tool.InputSchema
import com.anthropic.models.ToolChoiceTool
import scala.jdk.CollectionConverters._

object AnthropicToolsExample {
  def main(args: Array[String]): Unit = {
    val anthropicClient = AnthropicOkHttpClient.fromEnv()
    
    // Define a tool schema
    val weatherToolSchema = InputSchema.builder()
      .properties(JsonValue.from(Map(
        "location" -> Map(
          "type" -> "string",
          "description" -> "The city and country to get weather for"
        ),
        "unit" -> Map(
          "type" -> "string",
          "enum" -> List("celsius", "fahrenheit").asJava,
          "description" -> "The temperature unit to use"
        )
      ).asJava))
      .putAdditionalProperty("required", JsonValue.from(List("location").asJava))
      .build()
    
    // Create message with tool
    val messageParams = MessageCreateParams.builder()
      .model(Model.CLAUDE_3_7_SONNET_LATEST)
      .maxTokens(1024)
      .addTool(Tool.builder().name("get_weather").inputSchema(weatherToolSchema).build())
      .toolChoice(ToolChoiceTool.builder().name("get_weather").build())
      .addUserMessage("What's the weather like in Paris, France?")
      .build()
    
    // Get and process the response
    val response = anthropicClient.messages().create(messageParams)
    
    response.content().stream()
      .flatMap(contentBlock => contentBlock.toolUse().stream())
      .forEach(toolUseBlock => println(toolUseBlock._input()))
  }
}
```

For more advanced tool usage, you can implement a tool registry with handlers:

```scala
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.{ChatRequestMessage, ChatRequestUserMessage, ChatCompletionsOptions}
import scala.jdk.CollectionConverters._

object ToolRegistryExample {
  def main(args: Array[String]): Unit = {
    // Get Anthropic client
    val client = AnthropicOkHttpClient.fromEnv()
    
    // Create tools
    val weatherToolSchema = InputSchema.builder()
      .properties(JsonValue.from(Map(
        "location" -> Map(
          "type" -> "string",
          "description" -> "The city and country to get weather for"
        )
      ).asJava))
      .putAdditionalProperty("required", JsonValue.from(List("location").asJava))
      .build()
      
    val weatherTool = Tool.builder().name("get_weather").inputSchema(weatherToolSchema).build()
    
    // Create chat messages and options
    val chatMessages = new java.util.ArrayList[ChatRequestMessage]()
    chatMessages.add(new ChatRequestUserMessage("What's the weather like in Paris, France?"))
    
    val chatOptions = new ChatCompletionsOptions(chatMessages)
    
    // Add tools to message parameters
    val messageParams = MessageCreateParams.builder()
      .model(Model.CLAUDE_3_7_SONNET_LATEST)
      .maxTokens(1024)
      .addTool(weatherTool)
      .addUserMessage("What's the weather like in Paris, France?")
      .build()
    
    // Make the request and process response
    // ...
  }
}
```

## Advanced Configuration

### Customizing the Client

LLM4S provides several ways to customize the Anthropic client:

```scala
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import java.time.Duration

// Custom configuration
val customClient = AnthropicOkHttpClient.builder()
  .apiKey(System.getenv("ANTHROPIC_API_KEY"))
  .defaultTimeout(Duration.ofSeconds(60))
  .build()
```

### Schema Definitions

For complex parameter validation, use the Schema builder API:

```scala
// Using JsonValue for schema definition
val paramSchema = JsonValue.from(Map(
  "type" -> "object",
  "properties" -> Map(
    "query" -> Map(
      "type" -> "string",
      "description" -> "Search query",
      "minLength" -> 1,
      "maxLength" -> 100
    ),
    "temperature" -> Map(
      "type" -> "number",
      "description" -> "Model temperature",
      "minimum" -> 0.0,
      "maximum" -> 1.0
    )
  ),
  "required" -> List("query")
))
```

## Best Practices

1. **Client Reuse**: Create a single client instance and reuse it for all requests to maximize connection pool efficiency.

2. **Use Streaming for Long Responses**: Implement streaming for responses that might take a long time to generate.

3. **Handle Errors Gracefully**: Implement proper error handling for different types of exceptions.

4. **Validate Inputs**: Use schema definitions to validate inputs before sending requests.

5. **Security**: Never hardcode API keys in your application. Use environment variables or a secure configuration system.

## Error Handling

Implement comprehensive error handling for your Anthropic API calls:

```scala
import com.anthropic.errors._

try {
  val response = anthropicClient.messages().create(messageParams)
  // Process response
} catch {
  case e: AuthenticationException => 
    println("Authentication failed. Check your API key.")
  
  case e: RateLimitException =>
    println("Rate limit exceeded. Retry after: " + e.retryAfter.getOrElse("unknown"))
  
  case e: BadRequestException =>
    println("Invalid request: " + e.getMessage)
  
  case e: AnthropicServiceException =>
    println("API error: " + e.getMessage)
  
  case e: AnthropicIoException =>
    println("Network error: " + e.getMessage)
  
  case e: Throwable =>
    println("Unexpected error: " + e.getMessage)
}
```

---

By following this guide, you should be able to successfully integrate the Anthropic API with your Scala applications. For more advanced usage and detailed API documentation, refer to the official Anthropic Java SDK documentation at [javadoc.io](https://javadoc.io/doc/com.anthropic/anthropic-java).
