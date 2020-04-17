/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.server.controllers

import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.server.views.StatusPage
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class StatusPageController {
  @Autowired
  private lateinit var serverContext: ServerContext

  @Autowired
  private lateinit var taskManager: TaskManager

  @GetMapping("/")
  fun statusPageEndpoint() = StatusPage(serverContext, taskManager)
}