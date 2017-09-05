package org.jetbrains.plugins.verifier.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.io.Closeable
import java.io.File

/**
 * @author Sergey Patrikeev
 */
interface IdeFileLock : Closeable {

  val ideVersion: IdeVersion

  val ideFile: File

}