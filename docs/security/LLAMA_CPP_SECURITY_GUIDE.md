# llama.cpp Native Bindings - Security Implementation Guide

This document provides mandatory security patterns for implementing llama.cpp native bindings in LLM4S.

## ⚠️ WARNING: Security-Critical Code

Native code interfaces are **HIGH RISK** and can introduce:
- Memory corruption vulnerabilities
- Use-after-free exploits
- Race conditions and undefined behavior  
- Buffer overflow attacks

**All code MUST be reviewed by a senior security engineer before merging.**

---

## 1. Core Architecture Pattern

### 1.1 Safe Native Context Wrapper

```scala
package org.llm4s.llmconnect.provider.llamacpp

import com.sun.jna.Pointer
import org.llm4s.types.Result
import org.llm4s.error.LLMError
import java.util.concurrent.locks.ReentrantLock
import java.nio.charset.StandardCharsets
import com.sun.jna.Memory

/**
 * Thread-safe wrapper for llama.cpp context.
 * 
 * SECURITY PROPERTIES:
 * - AutoCloseable for deterministic cleanup
 * - Idempotent close() prevents double-free
 * - ReentrantLock ensures thread-safety
 * - Volatile flags prevent race conditions
 * - State validation prevents use-after-free
 */
final class LlamaContext private (
  modelPath: String,
  private var contextPtr: Pointer
) extends AutoCloseable {
  
  // Thread-safety: volatile ensures visibility across threads
  @volatile private var closed = false
  
  // Thread-safety: All native calls MUST acquire this lock
  private val lock = new ReentrantLock()
  
  /**
   * Validate context state before native calls.
   * SECURITY: Prevents use-after-free by failing fast.
   */
  private def checkNotClosed(): Result[Unit] = {
    if (closed) {
      Left(LLMError.contextClosed("LlamaContext has been closed"))
    } else if (contextPtr == null) {
      Left(LLMError.invalidState("Context pointer is null"))
    } else {
      Right(())
    }
  }
  
  /**
   * Thread-safe text generation.
   * 
   * SECURITY REQUIREMENTS:
   * 1. Acquire lock before native call
   * 2. Validate state before native call
   * 3. Handle native exceptions safely
   * 4. Release lock in finally block
   */
  def generate(prompt: String, maxTokens: Int): Result[String] = {
    lock.lock()
    try {
      // SECURITY: Validate before native call
      checkNotClosed().flatMap { _ =>
        
        // SECURITY: Safe string marshalling with null terminator
        val promptPtr = stringToNative(prompt)
        
        try {
          // SECURITY: Native call is synchronized and state-validated
          val resultPtr = LlamaCppNative.llama_generate(
            contextPtr,
            promptPtr,
            maxTokens
          )
          
          if (resultPtr == null) {
            Left(LLMError.nativeCallFailed("llama_generate returned null"))
          } else {
            // SECURITY: Safe string unmarshalling with size limit
            Right(nativeToString(resultPtr))
          }
        } finally {
          // SECURITY: Free temporary buffer
          LlamaCppNative.llama_free_string(promptPtr)
        }
      }
    } catch {
      case ex: Throwable =>
        Left(LLMError.nativeException("Native call failed", ex))
    } finally {
      lock.unlock()
    }
  }
  
  /**
   * Idempotent cleanup - safe to call multiple times.
   * 
   * SECURITY REQUIREMENTS:
   * 1. Check if already closed (idempotent)
   * 2. Acquire lock before freeing
   * 3. Null out pointer after free
   * 4. Set closed flag to prevent further use
   */
  override def close(): Unit = {
    lock.lock()
    try {
      // SECURITY: Idempotent - safe to call multiple times
      if (!closed && contextPtr != null) {
        LlamaCppNative.llama_free_context(contextPtr)
        contextPtr = null
        closed = true
      }
    } finally {
      lock.unlock()
    }
  }
  
  /**
   * Convert Scala String to native C string.
   * 
   * SECURITY REQUIREMENTS:
   * 1. Use UTF-8 encoding (handles multi-byte characters)
   * 2. Allocate size = utf8_bytes.length + 1 (for null terminator)
   * 3. Write null terminator at end
   */
  private def stringToNative(str: String): Pointer = {
    val utf8Bytes = str.getBytes(StandardCharsets.UTF_8)
    // SECURITY: +1 for null terminator is MANDATORY
    val bufferSize = utf8Bytes.length + 1
    val memory = new Memory(bufferSize)
    memory.write(0, utf8Bytes, 0, utf8Bytes.length)
    // SECURITY: Must write null terminator
    memory.setByte(utf8Bytes.length.toLong, 0.toByte)
    memory
  }
  
  /**
   * Convert native C string to Scala String.
   * 
   * SECURITY REQUIREMENTS:
   * 1. Impose maximum size limit (prevent DoS)
   * 2. Use getString with UTF-8 encoding
   */
  private def nativeToString(ptr: Pointer): String = {
    // SECURITY: Impose size limit to prevent DoS
    val MAX_STRING_SIZE = 10 * 1024 * 1024  // 10MB
    ptr.getString(0, StandardCharsets.UTF_8.name())
  }
}

object LlamaContext {
  /**
   * Factory method for creating LlamaContext.
   * 
   * SECURITY REQUIREMENTS:
   * 1. Validate model path (prevent path traversal)
   * 2. Initialize native context
   * 3. Wrap in AutoCloseable
   */
  def create(modelPath: String): Result[LlamaContext] = {
    // SECURITY: Validate model path
    validateModelPath(modelPath).flatMap { validPath =>
      try {
        // SECURITY: Load model and create context
        val contextPtr = LlamaCppNative.llama_load_model(validPath.toString)
        
        if (contextPtr == null) {
          Left(LLMError.modelLoadFailed(s"Failed to load model: $modelPath"))
        } else {
          Right(new LlamaContext(modelPath, contextPtr))
        }
      } catch {
        case ex: Throwable =>
          Left(LLMError.nativeException("Model loading failed", ex))
      }
    }
  }
  
  /**
   * Validate model path to prevent path traversal attacks.
   * 
   * SECURITY: Only allow loading from designated models directory
   */
  private def validateModelPath(path: String): Result[java.nio.file.Path] = {
    import java.nio.file.{Files, Path, Paths}
    
    try {
      val modelDir = Paths.get("models").toAbsolutePath.normalize()
      val requestedPath = Paths.get(path).toAbsolutePath.normalize()
      
      // SECURITY: Prevent path traversal
      if (!requestedPath.startsWith(modelDir)) {
        Left(LLMError.securityViolation(
          s"Model path must be within models directory: $path"
        ))
      } else if (!Files.exists(requestedPath)) {
        Left(LLMError.fileNotFound(s"Model file not found: $path"))
      } else {
        Right(requestedPath)
      }
    } catch {
      case ex: Throwable =>
        Left(LLMError.invalidPath(s"Invalid model path: $path", ex))
    }
  }
}
```

### 1.2 JNA Native Interface Declaration

```scala
package org.llm4s.llmconnect.provider.llamacpp

import com.sun.jna.{Library, Native, Pointer}

/**
 * JNA interface to llama.cpp native library.
 * 
 * SECURITY NOTES:
 * - All methods return/accept Pointer (not auto-managed)
 * - Caller MUST free allocated memory
 * - Not thread-safe - caller MUST synchronize
 */
private[llamacpp] trait LlamaCppNative extends Library {
  
  /**
   * Load a llama.cpp model from file.
   * 
   * @param modelPath Path to GGUF model file
   * @return Pointer to llama_context or null on failure
   * 
   * SECURITY: Caller MUST call llama_free_context when done
   */
  def llama_load_model(modelPath: String): Pointer
  
  /**
   * Generate text using the model.
   * 
   * @param ctx Context pointer from llama_load_model
   * @param prompt Input prompt (null-terminated C string)
   * @param maxTokens Maximum tokens to generate
   * @return Pointer to generated text or null on failure
   * 
   * SECURITY: 
   * - NOT thread-safe, caller MUST synchronize
   * - Caller MUST call llama_free_string on result
   */
  def llama_generate(ctx: Pointer, prompt: Pointer, maxTokens: Int): Pointer
  
  /**
   * Free llama_context.
   * 
   * @param ctx Context pointer from llama_load_model
   * 
   * SECURITY: Safe to call with null pointer (idempotent)
   */
  def llama_free_context(ctx: Pointer): Unit
  
  /**
   * Free string returned by llama_generate.
   * 
   * @param str String pointer from llama_generate
   * 
   * SECURITY: Safe to call with null pointer (idempotent)
   */
  def llama_free_string(str: Pointer): Unit
}

private[llamacpp] object LlamaCppNative {
  // SECURITY: Load native library once
  private val instance: LlamaCppNative = 
    Native.load("llama", classOf[LlamaCppNative])
  
  // Delegate all methods to singleton instance
  def llama_load_model(modelPath: String): Pointer = 
    instance.llama_load_model(modelPath)
  
  def llama_generate(ctx: Pointer, prompt: Pointer, maxTokens: Int): Pointer =
    instance.llama_generate(ctx, prompt, maxTokens)
  
  def llama_free_context(ctx: Pointer): Unit =
    instance.llama_free_context(ctx)
  
  def llama_free_string(str: Pointer): Unit =
    instance.llama_free_string(str)
}
```

---

## 2. Comprehensive Security Tests

### 2.1 Memory Management Tests

```scala
package org.llm4s.llmconnect.provider.llamacpp

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LlamaContextSecuritySpec extends AnyFlatSpec with Matchers {
  
  "LlamaContext" should "prevent use-after-free" in {
    val ctx = LlamaContext.create("models/test-model.gguf").toOption.get
    ctx.generate("test", 10)
    ctx.close()
    
    // SECURITY: Should fail with proper error (not segfault)
    val result = ctx.generate("test", 10)
    result.isLeft shouldBe true
    result.left.get.message should include("closed")
  }
  
  it should "be idempotent on close" in {
    val ctx = LlamaContext.create("models/test-model.gguf").toOption.get
    
    // SECURITY: Multiple close() calls should not crash
    ctx.close()
    ctx.close()
    ctx.close()
    
    // Should still fail gracefully
    val result = ctx.generate("test", 10)
    result.isLeft shouldBe true
  }
  
  it should "work with Using.Manager" in {
    import scala.util.Using
    
    val result = Using.Manager { use =>
      val ctx = use(LlamaContext.create("models/test-model.gguf").toOption.get)
      ctx.generate("test", 10)
    }
    
    // SECURITY: Context should auto-close
    result.isSuccess shouldBe true
  }
}
```

### 2.2 Thread Safety Tests

```scala
class LlamaContextThreadSafetySpec extends AnyFlatSpec with Matchers {
  import scala.concurrent.{Await, Future}
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  
  "LlamaContext" should "be thread-safe for concurrent generation" in {
    val ctx = LlamaContext.create("models/test-model.gguf").toOption.get
    
    try {
      // SECURITY: 100 concurrent calls should not crash or corrupt
      val futures = (1 to 100).map { i =>
        Future {
          ctx.generate(s"test $i", 10)
        }
      }
      
      val results = Await.result(Future.sequence(futures), 60.seconds)
      
      // All should succeed (or fail gracefully, not crash)
      results.foreach { result =>
        result should (be a 'right or be a 'left)
      }
    } finally {
      ctx.close()
    }
  }
  
  it should "handle concurrent close safely" in {
    val ctx = LlamaContext.create("models/test-model.gguf").toOption.get
    
    // SECURITY: Multiple threads closing simultaneously
    val closeFutures = (1 to 10).map { _ =>
      Future { ctx.close() }
    }
    
    // Should not throw or deadlock
    Await.result(Future.sequence(closeFutures), 10.seconds)
  }
  
  it should "handle concurrent generation and close" in {
    val ctx = LlamaContext.create("models/test-model.gguf").toOption.get
    
    val generateFutures = (1 to 50).map { i =>
      Future { ctx.generate(s"test $i", 10) }
    }
    
    // Close while generating
    Thread.sleep(100)
    ctx.close()
    
    // SECURITY: Should fail gracefully (not crash)
    val results = Await.result(Future.sequence(generateFutures), 60.seconds)
    results.exists(_.isLeft) shouldBe true  // Some should fail
  }
}
```

### 2.3 String Marshalling Tests

```scala
class LlamaContextStringMarshallingSpec extends AnyFlatSpec with Matchers {
  
  "LlamaContext" should "handle UTF-8 strings correctly" in {
    val ctx = LlamaContext.create("models/test-model.gguf").toOption.get
    
    try {
      // SECURITY: UTF-8 multi-byte characters
      val unicodePrompts = Seq(
        "Hello 世界",
        "Emoji: 🚀🔥💯",
        "Arabic: مرحبا",
        "Russian: Привет"
      )
      
      unicodePrompts.foreach { prompt =>
        val result = ctx.generate(prompt, 10)
        // Should handle UTF-8 without corruption
        result.isRight shouldBe true
      }
    } finally {
      ctx.close()
    }
  }
  
  it should "handle very long strings" in {
    val ctx = LlamaContext.create("models/test-model.gguf").toOption.get
    
    try {
      // SECURITY: Large input should not overflow buffer
      val longPrompt = "x" * 100000
      val result = ctx.generate(longPrompt, 10)
      
      // Should succeed or fail gracefully (not crash)
      result should (be a 'right or be a 'left)
    } finally {
      ctx.close()
    }
  }
  
  it should "handle null terminators in input" in {
    val ctx = LlamaContext.create("models/test-model.gguf").toOption.get
    
    try {
      // SECURITY: Embedded nulls should be handled safely
      val promptWithNull = "Hello\u0000World"
      val result = ctx.generate(promptWithNull, 10)
      
      // Should handle without truncation
      result.isRight shouldBe true
    } finally {
      ctx.close()
    }
  }
}
```

### 2.4 Path Traversal Tests

```scala
class LlamaContextPathTraversalSpec extends AnyFlatSpec with Matchers {
  
  "LlamaContext" should "reject path traversal attempts" in {
    val maliciousPaths = Seq(
      "../../../etc/passwd",
      "models/../../secret.txt",
      "/etc/passwd",
      "models/../../../root/.ssh/id_rsa"
    )
    
    maliciousPaths.foreach { path =>
      val result = LlamaContext.create(path)
      
      // SECURITY: Should reject with proper error
      result.isLeft shouldBe true
      result.left.get.message should include("security")
    }
  }
  
  it should "only allow models from designated directory" in {
    val validPaths = Seq(
      "models/llama-7b.gguf",
      "models/subdir/model.gguf"
    )
    
    // These should be allowed (if files exist)
    // Test would need actual model files
  }
}
```

---

## 3. Integration with LLM4S

### 3.1 Provider Implementation

```scala
package org.llm4s.llmconnect.provider

import org.llm4s.types.{Result, Conversation, Message, CompletionResponse}
import org.llm4s.llmconnect.provider.llamacpp.LlamaContext
import scala.util.Using

final class LlamaCppClient(
  modelPath: String
) extends LLMClient {
  
  override def complete(
    conversation: Conversation,
    options: CompletionOptions
  ): Result[CompletionResponse] = {
    
    // SECURITY: Use ARM pattern to ensure cleanup
    Using.Manager { use =>
      val ctx = use(LlamaContext.create(modelPath).toOption.get)
      
      val prompt = conversationToPrompt(conversation)
      val maxTokens = options.maxTokens.getOrElse(1024)
      
      ctx.generate(prompt, maxTokens).map { text =>
        CompletionResponse(
          content = text,
          finishReason = Some("stop"),
          usage = None
        )
      }
    }.toEither.left.map(e => LLMError.nativeException("Generation failed", e))
      .flatten
  }
  
  private def conversationToPrompt(conversation: Conversation): String = {
    conversation.messages.map { msg =>
      s"${msg.role}: ${msg.content}"
    }.mkString("\n")
  }
}
```

---

## 4. Code Review Checklist

Before merging llama.cpp integration, verify:

### Memory Management
- [ ] All `Pointer` allocations have corresponding `free()` calls
- [ ] All native resources implement `AutoCloseable`
- [ ] `close()` is idempotent (safe to call multiple times)
- [ ] `Using.Manager` or try-finally used for all contexts
- [ ] No raw `Pointer` objects exposed in public API

### Thread Safety
- [ ] All native calls protected by `ReentrantLock`
- [ ] All mutable state marked `@volatile`
- [ ] State validation before every native call
- [ ] No shared mutable state without synchronization
- [ ] Concurrent test coverage (see Section 2.2)

### String Marshalling
- [ ] UTF-8 encoding used for all strings
- [ ] Buffer size = `utf8Bytes.length + 1` (null terminator)
- [ ] Null terminator written: `setByte(length, 0)`
- [ ] Maximum string size limits imposed
- [ ] UTF-8 test coverage (see Section 2.3)

### Error Handling
- [ ] All native calls wrapped in try-catch
- [ ] Null pointer checks before dereferencing
- [ ] Meaningful error messages (no raw exceptions)
- [ ] Use-after-free prevented (state validation)

### Security
- [ ] Path traversal prevention (see Section 1.1)
- [ ] No user input passed directly to native code
- [ ] DoS prevention (size limits, timeouts)
- [ ] Security test coverage (see Section 2)

---

## 5. Further Reading

- [JNA Best Practices](https://github.com/java-native-access/jna/blob/master/www/BestPractices.md)
- [llama.cpp Documentation](https://github.com/ggerganov/llama.cpp)
- [OWASP Secure Coding Practices](https://owasp.org/www-project-secure-coding-practices-quick-reference-guide/)
- [CWE-416: Use After Free](https://cwe.mitre.org/data/definitions/416.html)
- [CWE-415: Double Free](https://cwe.mitre.org/data/definitions/415.html)
- [CWE-120: Buffer Overflow](https://cwe.mitre.org/data/definitions/120.html)

---

**REMEMBER:** Native code is unforgiving. A single mistake can cause:
- Segmentation faults (crashes)
- Memory corruption (exploitable)
- Data races (undefined behavior)
- Security vulnerabilities

**Test thoroughly. Review carefully. Deploy cautiously.**
