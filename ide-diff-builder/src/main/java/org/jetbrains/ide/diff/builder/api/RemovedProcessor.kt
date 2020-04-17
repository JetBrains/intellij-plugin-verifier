/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class RemovedProcessor : ApiDiffProcessor {

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
    if (oldMember != null && oldMember.isAccessible && (newMember == null || !newMember.isAccessible)) {
      if (newClass != null) {
        val memberMovedUp = hasSuperTypeMatchingPredicate(newClass, newResolver) { parentClass ->
          when (oldMember) {
            is Method -> {
              parentClass.methods.any {
                it.name == oldMember.name
                  && it.descriptor == oldMember.descriptor
                  && it.isStatic == oldMember.isStatic
                  && it.isAccessible
              }
            }
            is Field -> {
              parentClass.fields.any {
                it.name == oldMember.name
                  && it.descriptor == oldMember.descriptor
                  && it.isStatic == oldMember.isStatic
                  && it.isAccessible
              }
            }
            else -> false
          }
        }
        if (memberMovedUp) {
          return
        }
      }
      result += oldMember
    }
  }
}