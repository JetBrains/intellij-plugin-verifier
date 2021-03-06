/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.server.controllers

import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.server.configuration.properties.AuthorizationProperties
import org.jetbrains.plugins.verifier.service.server.exceptions.AuthenticationFailedException
import org.jetbrains.plugins.verifier.service.server.exceptions.InvalidStateChangeException
import org.jetbrains.plugins.verifier.service.server.exceptions.NotFoundException
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@EnableConfigurationProperties(AuthorizationProperties::class)
class ServiceController(
  private val authorizationProperties: AuthorizationProperties
) {
  @Autowired
  private lateinit var serverContext: ServerContext

  @PostMapping("/control-service")
  fun controlServiceEndpoint(
    @RequestParam("admin-password") adminPassword: String,
    @RequestParam("service-name") serviceName: String,
    @RequestParam command: String
  ): String {
    if (adminPassword != authorizationProperties.password) {
      throw AuthenticationFailedException("Incorrect password")
    }
    val service = serverContext.allServices.find { it.serviceName == serviceName }
      ?: throw NotFoundException("Service $serviceName is not found")
    changeServiceState(service, command)
    return "redirect:/"
  }

  private fun changeServiceState(
    service: BaseService,
    command: String
  ) {
    val success = when (command) {
      "start" -> service.start()
      "resume" -> service.resume()
      "pause" -> service.pause()
      else -> throw NotFoundException("Unknown command: $command")
    }

    if (!success) {
      throw InvalidStateChangeException(
        "Service's ${service.serviceName} state cannot be changed from ${service.getState()} by command '$command'"
      )
    }
  }
}