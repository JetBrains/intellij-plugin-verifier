package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.usages.deprecated.deprecationInfo
import com.jetbrains.pluginverifier.verifiers.resolution.*
import org.jetbrains.ide.diff.builder.filter.ClassFilter
import org.jetbrains.ide.diff.builder.ide.toSignature

class IdeDeprecatedApiBuilder(private val classFilter: ClassFilter) {

  fun buildDeprecatedApis(resolver: Resolver, classes: Set<String>): Set<ApiSignature> {
    val deprecatedApis = hashSetOf<ApiSignature>()
    for (className in classes) {
      if (isIgnoredClassName(className)) {
        continue
      }

      val classFile = resolver.resolveClassOrNull(className) ?: continue
      if (!classFile.isIgnored && classFile.deprecationInfo != null) {
        deprecatedApis += classFile.toSignature()
      }

      classFile.methods
        .filterNot { it.isIgnored || isMethodOverriding(it, resolver) }
        .filter { it.deprecationInfo != null }
        .mapTo(deprecatedApis) { it.toSignature() }

      classFile.fields
        .filterNot { it.isIgnored }
        .filter { it.deprecationInfo != null }
        .mapTo(deprecatedApis) { it.toSignature() }
    }
    return deprecatedApis
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