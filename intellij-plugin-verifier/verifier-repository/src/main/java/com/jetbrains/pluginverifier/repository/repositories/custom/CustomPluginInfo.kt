/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.safeEquals
import com.jetbrains.pluginverifier.misc.safeHashCode
import com.jetbrains.pluginverifier.repository.Browseable
import com.jetbrains.pluginverifier.repository.Downloadable
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.net.URL
import java.util.*

/**
 * [PluginInfo] for a plugin from [CustomPluginRepository].
 */
class CustomPluginInfo(
  pluginId: String,
  pluginName: String,
  version: String,
  vendor: String?,
  val repositoryUrl: URL,
  override val downloadUrl: URL,
  override val browserUrl: URL,
  val sourceCodeUrl: URL?,
  sinceBuild: IdeVersion?,
  untilBuild: IdeVersion?
) : Downloadable, Browseable, PluginInfo(
  pluginId,
  pluginName,
  version,
  sinceBuild,
  untilBuild,
  vendor
) {

  override fun equals(other: Any?) = other is CustomPluginInfo
    && pluginId == other.pluginId
    && version == other.version
    && downloadUrl.safeEquals(other.downloadUrl)

  override fun hashCode() = Objects.hash(pluginId, version, downloadUrl.safeHashCode())
}