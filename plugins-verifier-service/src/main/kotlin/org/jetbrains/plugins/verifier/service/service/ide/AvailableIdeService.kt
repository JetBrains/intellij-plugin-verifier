/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import java.util.concurrent.TimeUnit

/**
 * Service responsible for providing set of IDE versions
 * available for verification to the Marketplace.
 */
class AvailableIdeService(
  taskManager: TaskManager,
  private val protocol: AvailableIdeProtocol,
  private val ideRepository: IdeRepository
) : BaseService("AvailableIdeService", 0, 1, TimeUnit.MINUTES, taskManager) {

  override fun doServe() {
    val availableIdesTask = SendAvailableIdesTask(ideRepository, protocol)
    taskManager.enqueue(availableIdesTask)
  }

}