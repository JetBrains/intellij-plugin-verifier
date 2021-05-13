/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.custom

import java.net.URL

private const val PROPERTY_PREFIX = "com.jetbrains.plugin.verifier.repository.custom.properties"

enum class CustomPluginRepositoryProperties(private val propertyName: String) {

  EXCEPTION_ANALYZER_PLUGIN_REPOSITORY_URL("$PROPERTY_PREFIX.ea.repository.url"),
  EXCEPTION_ANALYZER_PLUGIN_SOURCE_CODE_URL("$PROPERTY_PREFIX.ea.source.code.url"),

  TEAM_CITY_PLUGIN_BUILD_SERVER_URL("$PROPERTY_PREFIX.teamcity.build.server.url"),
  TEAM_CITY_PLUGIN_SOURCE_CODE_URL("$PROPERTY_PREFIX.teamcity.source.code.url");

  fun getUrl(): URL? = URL("https://ea.jetbrains.com") // System.getProperty(propertyName)?.let { URL(it) }

}