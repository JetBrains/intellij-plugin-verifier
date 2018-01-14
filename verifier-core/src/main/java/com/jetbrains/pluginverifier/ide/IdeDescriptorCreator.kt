package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path

object IdeDescriptorCreator {

  fun createByPath(file: Path, ideVersion: IdeVersion?): IdeDescriptor {
    val ide = IdeManager.createManager().createIde(file.toFile(), ideVersion)
    val ideResolver = IdeResolverCreator.createIdeResolver(ide)
    return IdeDescriptor(ide, ideResolver)
  }

}