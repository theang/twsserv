package serv1.db

import org.snakeyaml.engine.v2.api.{Load, LoadSettings}

import java.util
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object DBSchemaUpgrade {
  val settings: LoadSettings = LoadSettings.builder().setLabel("Custom config").build()

  def getSchemaUpgradeCommand(from: String, to: String): String = {
    val key = s"upgrade_${from}_to_$to"

    val load: Load = new Load(settings)
    val is = getClass.getClassLoader.getResourceAsStream(Configuration.DATABASE_SCHEMA_UPGRADE_FILE)
    val dbUpgradeScripts: mutable.Map[String, String] = load.loadFromInputStream(is).asInstanceOf[util.LinkedHashMap[String, String]].asScala
    dbUpgradeScripts(key)
  }
}
