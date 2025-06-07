package org.llm4s.imagegeneration.model

/** Image size enumeration */
sealed trait ImageSize {
  def width: Int
  def height: Int
  def description: String = s"${width}x${height}"
}

object ImageSize {
  case object Square512 extends ImageSize {
    val width = 512
    val height = 512
  }
  case object Square1024 extends ImageSize {
    val width = 1024
    val height = 1024
  }
  case object Landscape768x512 extends ImageSize {
    val width = 768
    val height = 512
  }
  case object Portrait512x768 extends ImageSize {
    val width = 512
    val height = 768
  }
}

/** Image format enumeration */
sealed trait ImageFormat {
  def extension: String
  def mimeType: String
}

object ImageFormat {
  case object PNG extends ImageFormat {
    val extension = "png"
    val mimeType = "image/png"
  }
  case object JPEG extends ImageFormat {
    val extension = "jpg"
    val mimeType = "image/jpeg"
  }
}

/** Options for image generation */
case class ImageGenerationOptions(
  size: ImageSize = ImageSize.Square512,
  format: ImageFormat = ImageFormat.PNG,
  seed: Option[Long] = None,
  guidanceScale: Double = 7.5,
  inferenceSteps: Int = 20,
  negativePrompt: Option[String] = None
) 