/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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