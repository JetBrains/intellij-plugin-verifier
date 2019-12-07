package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class IntroducedProcessor : ApiDiffProcessor {

  val result: MutableList<ClassFileMember> = arrayListOf()

  @Suppress("DuplicatedCode")
  override fun process(
    oldClass: ClassFile?,
    oldMember: ClassFileMember?,
    newClass: ClassFile?,
    newMember: ClassFileMember?,
    oldResolver: Resolver,
    newResolver: Resolver
  ) {
    if (newMember != null && newMember.isAccessible && (oldMember == null || !oldMember.isAccessible)) {
      if (oldClass != null) {
        val memberMovedDown = hasSuperTypeMatchingPredicate(oldClass, oldResolver) { parentClass ->
          when (newMember) {
            is Method -> {
              parentClass.methods.any {
                it.name == newMember.name
                  && it.descriptor == newMember.descriptor
                  && it.isStatic == newMember.isStatic
                  && it.isAccessible
              }
            }
            is Field -> {
              parentClass.fields.any {
                it.name == newMember.name
                  && it.descriptor == newMember.descriptor
                  && it.isStatic == newMember.isStatic
                  && it.isAccessible
              }
            }
            else -> false
          }
        }
        if (memberMovedDown) {
          return
        }
      }
      result += newMember
    }
  }
}