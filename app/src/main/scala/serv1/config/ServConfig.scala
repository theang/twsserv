package serv1.config

import com.typesafe.config.{Config, ConfigFactory}

import java.io.File

object ServConfig {
  private val baseConfig: Config = ConfigFactory.load()
  private val secretConfigProperty = "SECRET_CONFIG"
  private val defaultSecretConfigFile = "secrets.conf"
  val config: Config = ConfigFactory.parseFile(new File(System.getProperty(secretConfigProperty, defaultSecretConfigFile))).withFallback(baseConfig)
}
