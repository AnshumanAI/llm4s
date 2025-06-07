package org.llm4s.imagegeneration.model

import java.time.Instant
import java.nio.file.Path

/** Represents a generated image */
case class GeneratedImage(
  /** Base64 encoded image data */
  data: String,
  /** Image format */
  format: ImageFormat,
  /** Image dimensions */
  size: ImageSize,
  /** Generation timestamp */
  createdAt: Instant = Instant.now(),
  /** Original prompt used */
  prompt: String,
  /** Seed used for generation (if available) */
  seed: Option[Long] = None,
  /** Optional file path if saved to disk */
  filePath: Option[Path] = None
) {
  
  /** Get the image data as bytes */
  def asBytes: Array[Byte] = {
    import java.util.Base64
    Base64.getDecoder.decode(data)
  }
  
  /** Save image to file and return updated GeneratedImage with file path */
  def saveToFile(path: Path): Either[ImageGenerationError, GeneratedImage] = {
    try {
      import java.nio.file.Files
      Files.write(path, asBytes)
      Right(copy(filePath = Some(path)))
    } catch {
      case e: Exception => 
        Left(UnknownError(e))
    }
  }
} 