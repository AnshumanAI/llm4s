package org.llm4s.llmconnect.provider

import org.llm4s.llmconnect.model.{ EmbeddingRequest, EmbeddingResponse, EmbeddingError }

/**
 * Text-only embedding provider interface (e.g., OpenAI, VoyageAI).
 * Multimedia is handled locally via the UniversalEncoder façade.
 */
trait EmbeddingProvider {
  def embed(request: EmbeddingRequest): Either[EmbeddingError, EmbeddingResponse]
}
