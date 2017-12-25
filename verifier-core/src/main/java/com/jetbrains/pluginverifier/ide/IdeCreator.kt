package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * @author Sergey Patrikeev
 */
object IdeCreator {

  private val LOG: Logger = LoggerFactory.getLogger(IdeCreator::class.java)

  fun createByFile(file: Path, ideVersion: IdeVersion?): IdeDescriptor {
    LOG.debug("Reading IDE class files from $file")
    val ide = IdeManager.createManager().createIde(file.toFile(), ideVersion)
    val ideResolver = IdeResolverCreator.createIdeResolver(ide)
    return IdeDescriptor(ide, ideResolver)
  }

}