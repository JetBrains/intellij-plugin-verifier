package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.usages.experimental.isExperimentalApi
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

class ExperimentalProcessor : ApiDiffProcessor {

  val markedExperimental: MutableList<ClassFileMember> = arrayListOf()

  val unmarkedExperimental: MutableList<ClassFileMember> = arrayListOf()

  override fun process(
      oldMember: ClassFileMember?,
      newMember: ClassFileMember?,
      oldResolver: Resolver,
      newResolver: Resolver
  ) {
    if (oldMember != null && oldMember.isAccessible && newMember != null && newMember.isAccessible) {
      val oldExperimental = oldMember.isExperimentalApi(oldResolver)
      val newExperimental = newMember.isExperimentalApi(newResolver)
      if (!oldExperimental && newExperimental) {
        markedExperimental += newMember
      }
      if (oldExperimental && !newExperimental) {
        unmarkedExperimental += oldMember
      }
    }
  }

}