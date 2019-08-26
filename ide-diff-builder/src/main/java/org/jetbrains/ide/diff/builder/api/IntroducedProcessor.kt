package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

class IntroducedProcessor : ApiDiffProcessor {

  val result: MutableList<ClassFileMember> = arrayListOf()

  override fun process(
      oldMember: ClassFileMember?,
      newMember: ClassFileMember?,
      oldResolver: Resolver,
      newResolver: Resolver
  ) {
    if (newMember != null && newMember.isAccessible && (oldMember == null || !oldMember.isAccessible)) {
      result += newMember
    }
  }
}