package org.jetbrains.plugins.verifier.service.ide

import com.intellij.structure.ide.IdeVersion
import java.io.Closeable
import java.io.File

/**
 * @author Sergey Patrikeev
 */
interface IdeFileLock : Closeable {

  val ideVersion: IdeVersion

  val ideFile: File

}