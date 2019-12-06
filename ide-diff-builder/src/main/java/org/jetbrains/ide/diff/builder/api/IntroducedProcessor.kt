package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

class IntroducedProcessor : ApiDiffProcessor {

  val result: MutableList<ClassFileMember> = arrayListOf()

  override fun process(
    oldClass: ClassFile?,
    oldMember: ClassFileMember?,
    newClass: ClassFile?,
    newMember: ClassFileMember?,
    oldResolver: Resolver,
    newResolver: Resolver
  ) {
    if (newMember != null && newMember.isAccessible && (oldMember == null || !oldMember.isAccessible)) {
      result += newMember
    }
  }
}