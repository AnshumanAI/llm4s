package org.llm4s.speech.model

import scala.util.control.NoStackTrace

/**
 * Speech processing errors
 * 
 * @author AnshumanAI
 */
sealed trait SpeechError extends Exception with NoStackTrace {
  def message: String
  override def getMessage: String = message
}

case class SpeechAuthenticationError(message: String) extends SpeechError
case class SpeechRateLimitError(message: String) extends SpeechError
case class SpeechValidationError(message: String) extends SpeechError
case class SpeechUnknownError(cause: Throwable) extends SpeechError {
  def message: String = s"Unknown speech error: ${cause.getMessage}"
}