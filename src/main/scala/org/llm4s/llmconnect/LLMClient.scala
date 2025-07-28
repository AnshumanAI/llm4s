package org.llm4s.llmconnect

import org.llm4s.llmconnect.model._

trait LLMClient {

  /** Complete a conversation and get a response */
  def complete(
    conversation: Conversation,
    options: CompletionOptions = CompletionOptions()
  ): Either[LLMError, Completion]

  /** Stream a completion with callback for chunks */
  def streamComplete(
    conversation: Conversation,
    options: CompletionOptions = CompletionOptions(),
    onChunk: StreamedChunk => Unit
  ): Either[LLMError, Completion]
}
