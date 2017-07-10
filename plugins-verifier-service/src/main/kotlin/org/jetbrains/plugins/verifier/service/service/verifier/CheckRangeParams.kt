package org.jetbrains.plugins.verifier.service.service.verifier

import com.google.gson.annotations.SerializedName
import org.jetbrains.plugins.verifier.service.params.JdkVersion

data class CheckRangeParams(@SerializedName("jdkVersion") val jdkVersion: JdkVersion)