package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

class RemovedProcessor : ApiDiffProcessor {

  val result: MutableList<ClassFileMember> = arrayListOf()

  override fun process(
      oldMember: ClassFileMember?,
      newMember: ClassFileMember?,
      oldResolver: Resolver,
      newResolver: Resolver
  ) {
    if (oldMember != null && oldMember.isAccessible && (newMember == null || !newMember.isAccessible)) {
      result += oldMember
    }
  }
}