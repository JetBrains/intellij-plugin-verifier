package org.jetbrains.plugins.verifier.service.server.controllers

import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.server.exceptions.AuthenticationFailedException
import org.jetbrains.plugins.verifier.service.server.exceptions.InvalidStateChangeException
import org.jetbrains.plugins.verifier.service.server.exceptions.NotFoundException
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class ServiceController {
  @Autowired
  private lateinit var serverContext: ServerContext

  @PostMapping("/control-service")
  fun controlServiceEndpoint(
      @RequestParam("admin-password") adminPassword: String,
      @RequestParam("service-name") serviceName: String,
      @RequestParam command: String
  ): String {
    if (adminPassword != serverContext.authorizationData.serviceAdminPassword) {
      throw AuthenticationFailedException("Incorrect password")
    }
    val service = serverContext.allServices.find { it.serviceName == serviceName }
        ?: throw NotFoundException("Service $serviceName is not found")
    changeServiceState(service, command)
    return "redirect:/info/status"
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