package org.jetbrains.plugins.verifier.service

class UrlMappings {

  static mappings = {
    "/$controller/$action?/$id?(.$format)?" {
      constraints {
        // apply constraints here
      }
    }

    "/"(view: "/index")
  }
}
