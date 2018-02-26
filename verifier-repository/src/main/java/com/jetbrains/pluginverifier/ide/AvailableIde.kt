package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.net.URL

/**
 * Descriptor of IDE available in the IDE repository,
 * which resides in [https://www.jetbrains.com/intellij-repository/releases]
 * and [https://www.jetbrains.com/intellij-repository/snapshots].
 */
data class AvailableIde(
    /**
     * IDE build number of _this_ IDE.
     */
    val version: IdeVersion,
    /**
     * Official release version of this IDE,
     * like `2017.3.4, 2017.3`, or `null`
     * if this IDE is not a release IDE.
     */
    val releasedVersion: String?,
    /**
     * Whether this IDE is from the /snapshots repository
     */
    val isSnapshot: Boolean,
    /**
     * Download URL pointing to the .zip archive of this IDE.
     */
    val downloadUrl: URL
) {
  override fun toString() = version.toString() + if (isSnapshot) " (snapshot)" else ""

  val isRelease: Boolean
    get() = releasedVersion != null
}