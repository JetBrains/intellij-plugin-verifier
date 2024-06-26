/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.nonExtendable

import com.jetbrains.pluginverifier.verifiers.hasAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

fun ClassFileMember.isNonExtendable(): Boolean =
  annotations.hasAnnotation("org/jetbrains/annotations/ApiStatus\$NonExtendable")