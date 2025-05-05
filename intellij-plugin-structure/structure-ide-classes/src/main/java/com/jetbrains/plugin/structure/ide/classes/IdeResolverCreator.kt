/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.InvalidIdeException

object IdeResolverCreator {
  private val distributionIdeResolverProvider = DistributionIdeResolverProvider()

  @JvmStatic
  fun createIdeResolver(ide: Ide): Resolver = createIdeResolver(Resolver.ReadMode.FULL, ide)

  @JvmStatic
  fun createIdeResolver(readMode: Resolver.ReadMode, ide: Ide): Resolver = createIdeResolver(ide, IdeResolverConfiguration(readMode))

  @Throws(InvalidIdeException::class)
  @JvmStatic
  fun createIdeResolver(ide: Ide, configuration: IdeResolverConfiguration): Resolver {
    val idePath = ide.idePath
    if (distributionIdeResolverProvider.supports(idePath)) {
      return distributionIdeResolverProvider.getIdeResolver(ide, configuration)
    } else {
      throw InvalidIdeException(idePath, "Invalid IDE $ide at $idePath")
    }
  }
}

