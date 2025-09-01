package org.llm4s.llmconnect.model

/**
 * Successful response carrying embedding vectors and lightweight metadata.
 *
 * @param embeddings  One vector per input text/chunk (or per item).
 * @param metadata    Provider/model info etc. (e.g., "provider" -> "openai", "model" -> "...").
 * @param modality    Optional overall modality tag (Text, Audio, Video) when known.
 * @param dim         Optional dimensionality, if convenient to surface at response-level.
 *
 * Notes:
 * - Defaults on `metadata`, `modality`, and `dim` keep old call-sites source-compatible.
 * - Providers can set `modality`/`dim` when they know it; callers can ignore safely.
 */
final case class EmbeddingResponse(
  embeddings: Seq[Seq[Double]],
  metadata: Map[String, String] = Map.empty,
  modality: Option[Modality] = None,
  dim: Option[Int] = None
) {

  /** Back-compat alias for older code paths that expect `vectors`. */
  @deprecated("Use `embeddings` instead of `vectors` (Embedx-v2).", "embedx-v2")
  def vectors: Seq[Seq[Double]] = embeddings
}

object EmbeddingResponse {

  /** Convenience factory for an empty response. */
  val empty: EmbeddingResponse = EmbeddingResponse(Nil)
}
