package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.safeEquals
import com.jetbrains.pluginverifier.misc.safeHashCode
import java.net.URL
import java.time.LocalDate
import java.util.*

/**
 * Descriptor of IDE build available for downloading
 * from source repository, such as https://download.jetbrains.com
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
    val releaseVersion: String?,
    /**
     * URL to download this IDE build.
     */
    val downloadUrl: URL,
    /**
     * Date when this IDE was uploaded to repository.
     */
    val uploadDate: LocalDate
) {

  override fun toString() = version.toString() + (if (isRelease) " ($releaseVersion)" else "") + " $uploadDate"

  val isRelease: Boolean
    get() = releaseVersion != null

  override fun equals(other: Any?) = other is AvailableIde
      && version == other.version
      && releaseVersion == other.releaseVersion
      && uploadDate == other.uploadDate
      && downloadUrl.safeEquals(other.downloadUrl)

  override fun hashCode() = Objects.hash(version, releaseVersion, uploadDate, downloadUrl.safeHashCode())
}