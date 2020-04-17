/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URL

@ConfigurationProperties("verifier.service.plugins.repository")
class PluginRepositoryProperties {
  lateinit var url: URL
  lateinit var token: String
}