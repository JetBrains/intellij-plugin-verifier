/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.ide.IntelliJPlatformProduct
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.network.safeEquals
import com.jetbrains.pluginverifier.network.safeHashCode
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
  val uploadDate: LocalDate,
  /**
   * IntelliJ Platform Product descriptor.
   */
  val product: IntelliJPlatformProduct
) {

  override fun toString() = version.toString() + (if (isRelease) " ($releaseVersion)" else "")

  val isRelease: Boolean
    get() = releaseVersion != null

  override fun equals(other: Any?) = other is AvailableIde
    && version == other.version
    && releaseVersion == other.releaseVersion
    && uploadDate == other.uploadDate
    && downloadUrl.safeEquals(other.downloadUrl)

  override fun hashCode() = Objects.hash(version, releaseVersion, uploadDate, downloadUrl.safeHashCode())
}