package com.jetbrains.pluginverifier.client.util

import com.jetbrains.pluginverifier.client.BaseCmdOpts
import org.jetbrains.plugins.verifier.service.params.JdkVersion

/**
 * @author Sergey Patrikeev
 */
object BaseCmdUtil {
  fun parseJdkVersion(opts: BaseCmdOpts): JdkVersion? {
    if (opts.jdkVersion == null) {
      return null
    }
    val jdkVersion: JdkVersion = when (opts.jdkVersion) {
      6 -> JdkVersion.JAVA_6_ORACLE
      7 -> JdkVersion.JAVA_7_ORACLE
      8 -> JdkVersion.JAVA_8_ORACLE
      else -> {
        throw IllegalArgumentException("Unsupported JDK version ${opts.jdkVersion}")
      }
    }
    return jdkVersion
  }

}