/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.pluginverifier.ide.AvailableIde

/**
 * Protocol of communication with Marketplace
 * that allows to send available IDE builds
 * to schedule verifications.
 */
interface AvailableIdeProtocol {

  fun sendAvailableIdes(availableIdes: List<AvailableIde>)

}