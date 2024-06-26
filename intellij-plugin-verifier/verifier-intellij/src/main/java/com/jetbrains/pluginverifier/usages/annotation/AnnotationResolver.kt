package com.jetbrains.pluginverifier.usages.annotation

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.usages.util.MemberAnnotation
import com.jetbrains.pluginverifier.usages.util.MemberAnnotation.*
import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.FullyQualifiedClassName
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull

class AnnotationResolver(val annotation: FullyQualifiedClassName) {
  fun resolve(classFileMember: ClassFileMember, classResolver: Resolver): MemberAnnotation? {
    resolveDirectAnnotation(classFileMember, classResolver)?.let { return it }
    return when (classFileMember) {
      is ClassFile -> {
        if (classFileMember.name.endsWith("package-info")) return null
        resolveInEnclosingClassName(classFileMember, classResolver)?.let { return it }
        resolveInPackageInfo(classFileMember, classResolver)?.let { return it }
        null
      }
      else -> resolveInNonClassFile(classFileMember, classResolver)
    }
  }

  private fun resolveDirectAnnotation(classFileMember: ClassFileMember, classResolver: Resolver): MemberAnnotation? =
    AnnotatedDirectly(classFileMember, annotation).takeIf { classFileMember.isDirectlyAnnotatedWith(annotation) }

  private fun resolveInNonClassFile(classFileMember: ClassFileMember, classResolver: Resolver): MemberAnnotation? =
    resolve(classFileMember.containingClassFile, classResolver)?.let {
      AnnotatedViaContainingClass(classFileMember.containingClassFile, classFileMember, annotation)
    }

  private fun resolveInEnclosingClassName(classFileMember: ClassFile, classResolver: Resolver): MemberAnnotation? {
    val enclosingClassName = classFileMember.enclosingClassName
    // If enclosing class name is the same as the current class name the endless loop happens
    // (since the same class will be resolved and findEffectiveMemberAnnotation call leads here)
    if (enclosingClassName == null || enclosingClassName == classFileMember.name) return null
    val enclosingClass = classResolver.resolveClassOrNull(enclosingClassName) ?: return null
    return resolve(enclosingClass, classResolver)?.let {
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

  private fun ClassFileMember.isDirectlyAnnotatedWith(annotationName: String): Boolean =
    annotations.findAnnotation(annotationName) != null
}

fun ClassFileMember.isMemberEffectivelyAnnotatedWith(annotationResolver: AnnotationResolver, classResolver: Resolver): Boolean =
  annotationResolver.resolve(this, classResolver) != null