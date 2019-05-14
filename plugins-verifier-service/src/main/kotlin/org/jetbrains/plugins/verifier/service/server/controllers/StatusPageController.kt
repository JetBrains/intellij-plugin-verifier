package org.jetbrains.plugins.verifier.service.server.controllers

import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.server.views.StatusPage
import org.jetbrains.plugins.verifier.service.startup.ServerStartupListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.View
import javax.servlet.ServletContext

@Controller
class StatusPageController {
  @Autowired
  private lateinit var servletContext: ServletContext

  @GetMapping("/")
  fun statusPageEndpoint(): View {
    val serverContext = servletContext.getAttribute(ServerStartupListener.SERVER_CONTEXT_KEY) as ServerContext
    return StatusPage(serverContext)
  }
}