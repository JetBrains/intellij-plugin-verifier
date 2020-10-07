/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.server.controllers

import org.jetbrains.plugins.verifier.service.server.configuration.properties.AuthorizationProperties
import org.jetbrains.plugins.verifier.service.server.exceptions.AuthenticationFailedException
import org.jetbrains.plugins.verifier.service.setting.DebugModeToggle
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@EnableConfigurationProperties(AuthorizationProperties::class)
class ToggleController(
  private val authorizationProperties: AuthorizationProperties
) {
  @PostMapping("/control-toggle")
  fun controlServiceEndpoint(
    @RequestParam("admin-password") adminPassword: String,
    @RequestParam("toggle-name") toggleName: String,
    @RequestParam("command") command: String
  ): String {
    if (adminPassword != authorizationProperties.password) {
      throw AuthenticationFailedException("Incorrect password")
    }
    check(toggleName == "debugMode") { toggleName }
    when (command) {
      "enable" -> DebugModeToggle.enable()
      "disable" -> DebugModeToggle.disable()
      else -> error(command)
    }
    return "redirect:/"
  }

}