package org.jetbrains.plugins.verifier.service

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.springframework.boot.actuate.health.DiskSpaceHealthIndicatorProperties

class Application extends GrailsAutoConfiguration {

  static void main(String[] args) {
    GrailsApp.run(Application, args)
  }

  @Override
  Closure doWithSpring() {
    { ->
      diskSpaceHealthIndicatorProperties(DiskSpaceHealthIndicatorProperties) {
        // Set threshold to 1 GB.
        threshold = 1024 * 1024 * 1024
      }
    }
  }
}