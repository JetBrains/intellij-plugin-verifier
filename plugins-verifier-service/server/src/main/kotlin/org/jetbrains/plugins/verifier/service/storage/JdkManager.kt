package org.jetbrains.plugins.verifier.service.storage

import org.jetbrains.plugins.verifier.service.params.JdkVersion
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.setting.get
import java.io.File

object JdkManager {

  fun getJdkHome(version: JdkVersion): File {
    return when (version) {
      JdkVersion.JAVA_6_ORACLE -> File(Settings[Settings.JDK_6_HOME])
      JdkVersion.JAVA_7_ORACLE -> File(Settings[Settings.JDK_7_HOME])
      JdkVersion.JAVA_8_ORACLE -> File(Settings[Settings.JDK_8_HOME])
    }
  }
}