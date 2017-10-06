package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.parameters.IdeDescriptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Sergey Patrikeev
 */
object IdeCreator {

  private val LOG: Logger = LoggerFactory.getLogger(IdeCreator::class.java)

  fun createByFile(file: File, ideVersion: IdeVersion?): IdeDescriptor {
    LOG.debug("Reading IDE class files from $file")
    val ide = IdeManager.createManager().createIde(file, ideVersion)
    val ideResolver = IdeResolverCreator.createIdeResolver(ide)
    return IdeDescriptor(ide, ideResolver)
  }

}