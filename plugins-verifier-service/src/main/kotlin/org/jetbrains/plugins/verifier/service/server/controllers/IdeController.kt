package org.jetbrains.plugins.verifier.service.server.controllers

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.startup.ServerStartupListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.ServletContext

@RestController
class IdeController {
  @Autowired
  private lateinit var servletContext: ServletContext

  @GetMapping("/ide/*")
  fun getAvailableIdes(): Set<IdeVersion> {
    val serverContext = servletContext.getAttribute(ServerStartupListener.SERVER_CONTEXT_KEY) as ServerContext
    return serverContext.ideFilesBank.getAvailableIdeVersions()
  }
}