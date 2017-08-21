package com.jetbrains.pluginverifier.ide

import com.intellij.structure.ide.IdeManager
import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import java.io.File

/**
 * @author Sergey Patrikeev
 */
object IdeCreator {

  fun createByFile(file: File, ideVersion: IdeVersion?): IdeDescriptor {
    val ide = IdeManager.getInstance().createIde(file, ideVersion)
    val ideResolver = Resolver.createIdeResolver(ide)
    return IdeDescriptor(ide, ideResolver)
  }

}