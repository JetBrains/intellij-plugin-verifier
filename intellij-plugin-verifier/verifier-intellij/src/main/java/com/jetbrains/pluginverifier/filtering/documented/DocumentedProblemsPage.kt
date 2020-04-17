/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering.documented

import java.net.URL

data class DocumentedProblemsPage(
  val webPageUrl: URL,
  val sourcePageUrl: URL,
  val editPageUrl: URL,
  val pageBody: String
)