package org.jetbrains.plugins.verifier.service.params

import com.google.gson.annotations.SerializedName

enum class JdkVersion {
  JAVA_6_ORACLE,
  JAVA_7_ORACLE,
  JAVA_8_ORACLE
}

data class CheckRangeRunnerParams(@SerializedName("jdkVersion") val jdkVersion: JdkVersion)