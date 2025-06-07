package org.llm4s.imagegeneration.model

sealed trait ImageGenerationError {
  def message: String
}

case class AuthenticationError(message: String) extends ImageGenerationError
case class RateLimitError(message: String) extends ImageGenerationError
case class ServiceError(message: String, code: Int) extends ImageGenerationError
case class ValidationError(message: String) extends ImageGenerationError
case class InvalidPromptError(message: String) extends ImageGenerationError
case class InsufficientResourcesError(message: String) extends ImageGenerationError
case class UnknownError(throwable: Throwable) extends ImageGenerationError {
  def message: String = throwable.getMessage
} 