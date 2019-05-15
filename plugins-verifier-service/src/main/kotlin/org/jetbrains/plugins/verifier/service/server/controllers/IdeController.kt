package org.jetbrains.plugins.verifier.service.server.controllers

import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class IdeController {
  @Autowired
  private lateinit var serverContext: ServerContext

  @GetMapping("/ide/*")
  fun getAvailableIdes() = serverContext.ideFilesBank.getAvailableIdeVersions()
}