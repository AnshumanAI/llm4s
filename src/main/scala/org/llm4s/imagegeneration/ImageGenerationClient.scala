package org.llm4s.imagegeneration

import org.llm4s.imagegeneration.model._

trait ImageGenerationClient {
  
  /** Generate an image from a text prompt */
  def generateImage(
    prompt: String,
    options: ImageGenerationOptions = ImageGenerationOptions()
  ): Either[ImageGenerationError, GeneratedImage]
  
  /** Generate multiple images from a text prompt */
  def generateImages(
    prompt: String,
    count: Int,
    options: ImageGenerationOptions = ImageGenerationOptions()
  ): Either[ImageGenerationError, Seq[GeneratedImage]]
  
  /** Check the health/status of the image generation service */
  def health(): Either[ImageGenerationError, ServiceStatus]
} 