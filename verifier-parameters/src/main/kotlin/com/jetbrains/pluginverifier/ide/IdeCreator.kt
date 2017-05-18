package com.jetbrains.pluginverifier.ide

import com.intellij.structure.ide.IdeManager
import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.resolvers.Resolver
import java.io.File

/**
 * @author Sergey Patrikeev
 */
object IdeCreator {

  fun createByFile(file: File, ideVersion: IdeVersion?): CreateIdeResult {
    val ide = IdeManager.getInstance().createIde(file, ideVersion)
    val ideResolver = Resolver.createIdeResolver(ide)
    return CreateIdeResult(ide, ideResolver)
  }

}