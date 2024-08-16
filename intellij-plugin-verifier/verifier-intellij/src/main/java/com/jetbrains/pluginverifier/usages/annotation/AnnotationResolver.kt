/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.pluginverifier.usages.annotation

import com.jetbrains.plugin.structure.classes.resolvers.DirectoryFileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.JarOrZipFileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.toFullJavaClassName
import com.jetbrains.pluginverifier.usages.util.MemberAnnotation
import com.jetbrains.pluginverifier.usages.util.MemberAnnotation.*
import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.FullyQualifiedClassName
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(AnnotationResolver::class.java)

class AnnotationResolver(val annotation: FullyQualifiedClassName) {

  fun resolve(classFileMember: ClassFileMember, classResolver: Resolver, usageLocation: Location?): MemberAnnotation? {
    return resolve(classFileMember, classResolver, ResolutionStack(annotation, usageLocation))
  }

  private fun resolve(classFileMember: ClassFileMember, classResolver: Resolver, resolutionStack: ResolutionStack): MemberAnnotation? = resolutionStack.execute(classFileMember) {
    resolveDirectAnnotation(classFileMember, classResolver)?.let { return it }
    when (classFileMember) {
      is ClassFile -> {
        if (classFileMember.name.endsWith("package-info")) return null
        resolveInEnclosingClassName(classFileMember, classResolver, resolutionStack)?.let { return it }
        resolveInPackageInfo(classFileMember, classResolver)?.let { return it }
        null
      }
      else -> resolveInNonClassFile(classFileMember, classResolver, resolutionStack)
    }
  }

  private fun resolveDirectAnnotation(classFileMember: ClassFileMember, classResolver: Resolver): MemberAnnotation? =
    AnnotatedDirectly(classFileMember, annotation).takeIf { classFileMember.isDirectlyAnnotatedWith(annotation) }

  private fun resolveInNonClassFile(classFileMember: ClassFileMember, classResolver: Resolver, resolutionStack: ResolutionStack): MemberAnnotation? =
    resolve(classFileMember.containingClassFile, classResolver, resolutionStack)?.let {
      AnnotatedViaContainingClass(classFileMember.containingClassFile, classFileMember, annotation)
    }

  private fun resolveInEnclosingClassName(classFileMember: ClassFile, classResolver: Resolver, resolutionStack: ResolutionStack): MemberAnnotation? {
    val enclosingClassName = classFileMember.enclosingClassName
    // If enclosing class name is the same as the current class name the endless loop happens
    // (since the same class will be resolved and findEffectiveMemberAnnotation call leads here)
    if (enclosingClassName == null || enclosingClassName == classFileMember.name) return null
    val enclosingClass = classResolver.resolveClassOrNull(enclosingClassName) ?: return null
    return resolve(enclosingClass, classResolver, resolutionStack)?.let {
      AnnotatedViaContainingClass(enclosingClass, classFileMember, annotation)
    }
  }

  private fun resolveInPackageInfo(classFileMember: ClassFile, classResolver: Resolver): MemberAnnotation? {
    val packageName = classFileMember.containingClassFile.packageName
    return packageName
      .takeIf { it.isNotEmpty() }
      ?.let { classResolver.resolveClassOrNull("$packageName/package-info") }
      ?.takeIf { it.isDirectlyAnnotatedWith(annotation) }
      ?.let { AnnotatedViaPackage(packageName, classFileMember, annotation) }
  }

  private inline fun ResolutionStack.execute(classFileMember: ClassFileMember, block: (ResolutionStack) -> MemberAnnotation?): MemberAnnotation? {
    return try {
      push(classFileMember)
      block(this).also {
        if (LOG.isDebugEnabled && it != null) {
          LOG.debug("Resolving annotation '{}': {}", annotation, toString())
        }
      }
    } finally {
      pop()
    }
  }

  private fun ClassFileMember.isDirectlyAnnotatedWith(annotationName: String): Boolean =
    annotations.findAnnotation(annotationName) != null


  private class ResolutionStack(val annotation: FullyQualifiedClassName, private val usageLocation: Location?) {
    private val stack = ArrayDeque<ClassFileMember>()

    fun push(m: ClassFileMember) {
      stack.add(m)
    }

    fun pop(): ClassFileMember? = stack.removeLastOrNull()

    override fun toString(): String {
      val prefix = if (usageLocation != null) "${usageLocation.presentableLocation} => " else ""

      return prefix + stack.joinToString(separator = " -> ") {
        when (it) {
          is ClassFile -> toFullJavaClassName(it.name) + " [" + it.getFileName() + "]"
          is Method -> toFullJavaClassName(it.containingClassFile.name) + "#" + it.name + " [" + it.getFileName() + "]"
          is Field -> toFullJavaClassName(it.containingClassFile.name) + "#" + it.name + " [" + it.getFileName() + "]"
          else -> it.toString()
        }
      }
    }

    private fun ClassFileMember.getFileName(): String {
      return containingClassFile.classFileOrigin.getFileName()
    }

    fun FileOrigin.getFileName(): String {
      return when (this) {
        is JarOrZipFileOrigin -> fileName
        is DirectoryFileOrigin -> directoryName
        else -> toString()
      }
    }
  }
}

fun ClassFileMember.isMemberEffectivelyAnnotatedWith(annotationResolver: AnnotationResolver, classResolver: Resolver): Boolean =
  isMemberEffectivelyAnnotatedWith(annotationResolver, classResolver, usageLocation = null)

fun ClassFileMember.isMemberEffectivelyAnnotatedWith(annotationResolver: AnnotationResolver, classResolver: Resolver, usageLocation: Location?): Boolean =
  annotationResolver.resolve(this, classResolver, usageLocation) != null