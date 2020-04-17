/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.resources

/**
 * Descriptor of the resource in the [repository] [ResourceRepository]
 * containing the reference to the [resource] and the cached resource's [weight].
 */
open class ResourceInfo<out R, W : ResourceWeight<W>>(val resource: R, val weight: W)