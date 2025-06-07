package org.llm4s.imagegeneration.model

import java.time.Instant

/** Service health status */
sealed trait HealthStatus
object HealthStatus {
  case object Healthy extends HealthStatus
  case object Degraded extends HealthStatus
  case object Unhealthy extends HealthStatus
}

/** Represents the status of the image generation service */
case class ServiceStatus(
  status: HealthStatus,
  message: String,
  lastChecked: Instant = Instant.now(),
  queueLength: Option[Int] = None,
  averageGenerationTime: Option[Long] = None // in milliseconds
) 