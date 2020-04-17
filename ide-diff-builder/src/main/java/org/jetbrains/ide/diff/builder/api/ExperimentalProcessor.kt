/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.usages.experimental.isExperimentalApi
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

class ExperimentalProcessor : ApiDiffProcessor {

  val markedExperimental: MutableList<ClassFileMember> = arrayListOf()

  val unmarkedExperimental: MutableList<ClassFileMember> = arrayListOf()

  override fun process(
    oldClass: ClassFile?,
    oldMember: ClassFileMember?,
    newClass: ClassFile?,
    newMember: ClassFileMember?,
    oldResolver: Resolver,
    newResolver: Resolver
  ) {
    if ((oldMember == null || !oldMember.isAccessible || !oldMember.isExperimentalApi(oldResolver))
      && newMember != null
      && newMember.isAccessible
      && newMember.isExperimentalApi(newResolver)
    ) {
      markedExperimental += newMember
    }

    if (oldMember != null
      && oldMember.isAccessible
      && oldMember.isExperimentalApi(oldResolver)
      && (newMember == null || !newMember.isAccessible || !newMember.isExperimentalApi(newResolver))
    ) {
      unmarkedExperimental += oldMember
    }
  }

}