package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.io.Closeable
import java.nio.file.Path

/**
 * Holds IDE objects necessary for verification.
 *
 * - [ide] - instance of this IDE
 * - [ideResolver] - accessor of IDE class files
 * - [ideFileLock] - a lock to protect the IDE file from deletion.
 * It will be closed along with `this` descriptor.
 */
data class IdeDescriptor(
    val ide: Ide,
    val ideResolver: Resolver,
    val ideFileLock: FileLock?
) : Closeable {

  /**
   * Version of this IDE.
   */
  val ideVersion = ide.version

  override fun toString() = ideVersion.toString()

  override fun close() {
    ideResolver.closeLogged()
    ideFileLock.closeLogged()
  }

  companion object {
    /**
     * Creates [IdeDescriptor] for specified [idePath].
     * [ideVersion] is used to override the default version.
     * [ideFileLock] will be released when this [IdeDescriptor] is closed.
     */
    fun create(idePath: Path, ideVersion: IdeVersion?, ideFileLock: FileLock?): IdeDescriptor {
      val ide = IdeManager.createManager().createIde(idePath.toFile(), ideVersion)
      val ideResolver = IdeResolverCreator.createIdeResolver(ide)
      return IdeDescriptor(ide, ideResolver, ideFileLock)
    }

  }

}