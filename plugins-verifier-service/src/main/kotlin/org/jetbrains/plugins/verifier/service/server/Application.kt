package org.jetbrains.plugins.verifier.service.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer


@SpringBootApplication
class VerificationServiceApplication : SpringBootServletInitializer() {
  override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
    return application.sources(VerificationServiceApplication::class.java)
  }
}

fun main(args: Array<String>) {
  runApplication<VerificationServiceApplication>(*args)
}