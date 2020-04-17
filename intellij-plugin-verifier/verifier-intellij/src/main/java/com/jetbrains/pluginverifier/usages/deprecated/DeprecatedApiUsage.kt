/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.usages.ApiUsage

/**
 * Usage of `@Deprecated` or `@deprecated` API.
 */
abstract class DeprecatedApiUsage(val deprecationInfo: DeprecationInfo) : ApiUsage()