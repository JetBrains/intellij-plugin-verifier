package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.net.URL

data class AvailableIde(val version: IdeVersion,
                        val isRelease: Boolean,
                        val isCommunity: Boolean,
                        val isSnapshot: Boolean,
                        val downloadUrl: URL) {
  override fun toString(): String = version.toString() + if (isSnapshot) " (snapshot)" else ""
}