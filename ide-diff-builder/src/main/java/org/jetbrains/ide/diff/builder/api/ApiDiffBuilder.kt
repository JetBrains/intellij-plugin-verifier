/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.resolution.*
import org.jetbrains.ide.diff.builder.filter.ClassFilter

class ApiDiffBuilder(
  private val classFilter: ClassFilter,
  private val processors: List<ApiDiffProcessor>
) {

  fun buildDiff(
    oldResolver: Resolver,
    newResolver: Resolver,
    oldClasses: Set<String>,
    newClasses: Set<String>
  ) {
    for (className in (oldClasses + newClasses)) {
      if (isIgnoredClassName(className)) {
        continue
      }

      val oldClass = oldResolver.resolveClassOrNull(className)
      val newClass = newResolver.resolveClassOrNull(className)

      buildApiDiff(oldClass, newClass, oldResolver, newResolver) skip@{ oldMember, newMember ->
        if (oldMember != null && oldMember.isIgnored || newMember != null && newMember.isIgnored) {
          return@skip
        }
        processors.forEach { it.process(oldClass, oldMember, newClass, newMember, oldResolver, newResolver) }
      }
    }
  }

  private fun buildApiDiff(
    oldClass: ClassFile?,
    newClass: ClassFile?,
    oldResolver: Resolver,
    newResolver: Resolver,
    processor: (ClassFileMember?, ClassFileMember?) -> Unit
  ) {
    processor(oldClass, newClass)

    val oldMethods = oldClass?.methods.orEmpty().associateBy { it.name + it.descriptor }
    val newMethods = newClass?.methods.orEmpty().associateBy { it.name + it.descriptor }

    for (nameDescriptor in (oldMethods.keys + newMethods.keys)) {
      val oldMethod = oldMethods[nameDescriptor]
      val newMethod = newMethods[nameDescriptor]
      if (oldMethod != null && isMethodOverriding(oldMethod, oldResolver)
        || newMethod != null && isMethodOverriding(newMethod, newResolver)) {
        continue
      }
      processor(oldMethod, newMethod)
    }

    val oneFields = oldClass?.fields.orEmpty().associateBy { it.name + it.descriptor }
    val twoFields = newClass?.fields.orEmpty().associateBy { it.name + it.descriptor }

    for (nameDescriptor in (oneFields.keys + twoFields.keys)) {
      val oneField = oneFields[nameDescriptor]
      val twoField = twoFields[nameDescriptor]
      processor(oneField, twoField)
    }
  }

  private fun isSyntheticLikeName(name: String) = name.contains("$$")
    || name.endsWith("$")
    || name.substringAfterLast('$', "").toIntOrNull() != null

  private fun isIgnoredClassName(className: String): Boolean =
    isSyntheticLikeName(className) || !classFilter.shouldProcessClass(className)

  private val ClassFileMember.isIgnored: Boolean
    get() = when (this) {
      is ClassFile -> isIgnoredClassName(name) || isSynthetic
      is Method -> isClassInitializer || isBridgeMethod || isSynthetic || isSyntheticLikeName(name)
      is Field -> isSynthetic || isSyntheticLikeName(name)
      else -> true
    }
}

val ClassFileMember.isAccessible: Boolean
  get() = isPublic || isProtected