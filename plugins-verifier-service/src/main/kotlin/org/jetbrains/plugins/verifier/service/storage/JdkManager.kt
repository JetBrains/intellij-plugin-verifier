package org.jetbrains.plugins.verifier.service.storage

import org.jetbrains.plugins.verifier.service.setting.Settings
import java.io.File

enum class JdkVersion {
  JAVA_8_ORACLE
}

object JdkManager {

  fun getJdkHome(version: JdkVersion): File = when (version) {
    JdkVersion.JAVA_8_ORACLE -> File(Settings.JDK_8_HOME.get())
  }
}