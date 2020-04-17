/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import java.util.*

/**
 * Descriptor of a plugin and a target against which the plugin is to be verified.
 * [manually] indicates whether this verification has been scheduled by a user
 * meaning that it has more priority for execution than automatically scheduled ones.
 */
data class ScheduledVerification(
  val updateInfo: UpdateInfo,
  val availableIde: AvailableIde,
  val manually: Boolean = false
) {
  override fun toString() = "$availableIde against $updateInfo" + if (manually) " (manually)" else ""

  override fun equals(other: Any?) = other is ScheduledVerification &&
    updateInfo == other.updateInfo &&
    availableIde == other.availableIde

  override fun hashCode() = Objects.hash(updateInfo, availableIde)
}