package org.llm4s.llmconnect.model

sealed trait LLMError {
  def message: String
}
case class AuthenticationError(message: String)     extends LLMError
case class RateLimitError(message: String)          extends LLMError
case class ServiceError(message: String, code: Int) extends LLMError
case class ValidationError(message: String)         extends LLMError
case class UnknownError(throwable: Throwable) extends LLMError {
  def message: String = throwable.getMessage
}
