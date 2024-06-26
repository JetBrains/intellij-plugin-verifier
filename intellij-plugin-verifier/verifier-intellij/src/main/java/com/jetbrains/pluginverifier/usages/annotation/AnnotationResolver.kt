package com.jetbrains.pluginverifier.usages.annotation

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.usages.util.MemberAnnotation
import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.FullyQualifiedClassName
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull

class AnnotationResolver(val annotation: FullyQualifiedClassName) {
  fun resolve(classFileMember: ClassFileMember, classResolver: Resolver): MemberAnnotation? {
    if (classFileMember.isDirectlyAnnotatedWith(annotation)) {
      return MemberAnnotation.AnnotatedDirectly(classFileMember, annotation)
    }

    if (classFileMember !is ClassFile) {
      val memberAnnotation = resolve(classFileMember.containingClassFile, classResolver)
      return memberAnnotation?.let { MemberAnnotation.AnnotatedViaContainingClass(classFileMember.containingClassFile, classFileMember, annotation) }
    }

    if (classFileMember.name.endsWith("package-info")) {
      return null
    }

    val enclosingClassName = classFileMember.enclosingClassName
    // If enclosing class name is the same as the current class name the endless loop happens
    // (since the same class will be resolved and findEffectiveMemberAnnotation call leads here)
    if (enclosingClassName != null && enclosingClassName != classFileMember.name) {
      val enclosingClass = classResolver.resolveClassOrNull(enclosingClassName) ?: return null
      val memberAnnotation = resolve(enclosingClass, classResolver)
      return memberAnnotation?.let { MemberAnnotation.AnnotatedViaContainingClass(enclosingClass, classFileMember, annotation) }
    }

    val packageName = classFileMember.containingClassFile.packageName
    if (packageName.isNotEmpty()) {
      val packageInfoClass = classResolver.resolveClassOrNull("$packageName/package-info")
      if (packageInfoClass != null) {
        return if (packageInfoClass.isDirectlyAnnotatedWith(annotation)) {
          MemberAnnotation.AnnotatedViaPackage(packageName, classFileMember, annotation)
        } else {
          null
        }
      }
    }

    return null
  }

  private fun ClassFileMember.isDirectlyAnnotatedWith(annotationName: String): Boolean =
    annotations.findAnnotation(annotationName) != null
}

fun ClassFileMember.isMemberEffectivelyAnnotatedWith(annotationResolver: AnnotationResolver, classResolver: Resolver): Boolean =
  annotationResolver.resolve(this, classResolver) != null