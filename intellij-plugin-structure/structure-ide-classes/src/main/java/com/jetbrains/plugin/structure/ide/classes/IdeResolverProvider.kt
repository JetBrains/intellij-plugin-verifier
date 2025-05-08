/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import java.nio.file.Path

interface IdeResolverProvider {
  @Throws(InvalidIdeException::class)
  fun getIdeResolver(ide: Ide, configuration: IdeResolverConfiguration): Resolver

  fun supports(idePath: Path): Boolean
}