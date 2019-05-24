package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.ide.IdeDescriptor
import java.io.Closeable

/**
 * Interface of the parameters needed to run a [verification task] [Task].
 *
 * The parameters can hold allocated resources such as
 * [class files resolvers] [com.jetbrains.plugin.structure.classes.resolvers.Resolver],
 * [IDE descriptors] [IdeDescriptor] and [file locks] [com.jetbrains.pluginverifier.repository.files.FileLock].
 * Thus, the [parameters] [TaskParameters] must be closed after usage.
 */
interface TaskParameters : Closeable {

  val presentableText: String

}
