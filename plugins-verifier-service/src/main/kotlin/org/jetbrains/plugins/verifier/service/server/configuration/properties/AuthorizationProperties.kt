/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("verifier.service.admin")
class AuthorizationProperties {
  lateinit var password: String
}