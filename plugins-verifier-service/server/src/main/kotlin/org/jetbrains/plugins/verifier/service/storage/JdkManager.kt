package org.jetbrains.plugins.verifier.service.storage

import org.jetbrains.plugins.verifier.service.params.JdkVersion
import org.jetbrains.plugins.verifier.service.setting.Settings
import java.io.File

object JdkManager {

  fun getJdkHome(version: JdkVersion): File = when (version) {
    JdkVersion.JAVA_6_ORACLE -> File(Settings.JDK_6_HOME.get())
    JdkVersion.JAVA_7_ORACLE -> File(Settings.JDK_7_HOME.get())
    JdkVersion.JAVA_8_ORACLE -> File(Settings.JDK_8_HOME.get())
  }
}