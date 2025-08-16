package org.llm4s.config

import io.github.cdimascio.dotenv.Dotenv

object EnvLoader {
  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)
  private lazy val dotenv: Dotenv = Dotenv
    .configure()
    .ignoreIfMissing()
    .load()

  logger.info(s"Environment variables loaded from .env file ${EnvLoader.dotenv}")
  def get(key: String): Option[String] = {
    // First check system environment variables
    val systemEnv = sys.env.get(key)
    if (systemEnv.isDefined) {
      systemEnv
    } else {
      // Fall back to .env file
      Option(dotenv.get(key))
    }
  }

  def getOrElse(key: String, default: String): String =
    get(key).getOrElse(default)
}
