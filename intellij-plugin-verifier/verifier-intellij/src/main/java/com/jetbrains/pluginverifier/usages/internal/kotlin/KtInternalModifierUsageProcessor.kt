package com.jetbrains.pluginverifier.usages.internal.kotlin

import com.jetbrains.plugin.structure.classes.utils.KtClassResolver
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.modifiers.Modifiers.Modifier.*
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.internal.BaseInternalApiUsageProcessor
import com.jetbrains.pluginverifier.usages.internal.InternalUsageRegistrar
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

/**
 * ByteBuddy modifiers when using the filtering with delegation:
 * ```
 * MethodDelegation.withDefaultConfiguration().filter(...)
 * ```
 */
private val byteBuddyMethodDelegationModifiers = Modifiers.of(PUBLIC, STATIC, VOLATILE, BRIDGE, SYNTHETIC)

private val kotlinPlatformPackages = listOf("kotlin", "kotlinx")

internal const val KOTLIN_PUBLISHED_API_ANNOTATION_DESC = "Lkotlin/PublishedApi;"

class KtInternalModifierUsageProcessor(
  verificationContext: PluginVerificationContext,
  private val excludedPackages: List<String> = kotlinPlatformPackages
) : BaseInternalApiUsageProcessor(KtInternalUsageRegistrar(verificationContext)) {
  private val ktClassResolver = KtClassResolver()

  override fun isInternal(
    resolvedMember: ClassFileMember,
    context: VerificationContext,
    usageLocation: Location
  ): Boolean {
    if (!hasKotlinMetadataAnnotation(resolvedMember)) {
      return false
    }
    if (hasSameOrigin(resolvedMember, usageLocation)) {
      return false
    }
    if (isPublishedApi(resolvedMember)) {
      return false
    }
    if (isIgnored(usageLocation, resolvedMember)) {
      return false
    }
    val classFile = resolvedMember.selfOrContainingClassFile ?: return false
    val classNode = (classFile as? ClassFileAsm)?.asmNode ?: return false
    val ktClassNode = ktClassResolver[classNode] ?: return false

    return when (resolvedMember) {
      is ClassFile -> ktClassNode.isInternal
      is Field -> ktClassNode.isInternalField(resolvedMember.name)
      is Method -> ktClassNode.isInternalFunction(resolvedMember.name, resolvedMember.descriptor)
      else -> false
    }
  }

  private fun isIgnored(usageLocation: Location, resolvedMember: ClassFileMember): Boolean {
    return isKotlinPlatform(resolvedMember)
      || usageLocation is FieldLocation && usageLocation.modifiers == byteBuddyMethodDelegationModifiers
  }

  private fun isKotlinPlatform(resolvedMember: ClassFileMember): Boolean {
    val pkg = resolvedMember.containingClassFile.javaPackageName
    return excludedPackages
      .map { "$it." }
      .any {
        pkg.startsWith(it)
      }
  }

  private fun hasSameOrigin(
    resolvedMember: ClassFileMember,
    usageLocation: Location
  ) = resolvedMember.containingClassFile.classFileOrigin == usageLocation.containingClass.classFileOrigin

  private val ClassFileMember.selfOrContainingClassFile: ClassFile?
    get() = when (this) {
      is ClassFile -> this
      is Field -> containingClassFile
      is Method -> containingClassFile
      else -> null
    }

  private fun hasKotlinMetadataAnnotation(classFileMember: ClassFileMember): Boolean =
    classFileMember.selfOrContainingClassFile?.let {
      hasKotlinMetadataAnnotation(it)
    } ?: false

  private fun isPublishedApi(classFileMember: ClassFileMember): Boolean =
    classFileMember.annotations.any { it.desc == KOTLIN_PUBLISHED_API_ANNOTATION_DESC }

  private fun hasKotlinMetadataAnnotation(classFile: ClassFile): Boolean =
    classFile is ClassFileAsm && KtClassResolver.hasKotlinMetadataAnnotation(classFile.asmNode)

  class KtInternalUsageRegistrar(private val verificationContext: PluginVerificationContext) : InternalUsageRegistrar {
    override fun registerClass(classReference: ClassReference, apiElement: ClassLocation, usageLocation: Location) {
      verificationContext.registerInternalApiUsage(KtInternalClassUsage(classReference, apiElement, usageLocation))
    }

    override fun registerMethod(
      methodReference: MethodReference,
      apiElement: MethodLocation,
      usageLocation: MethodLocation
    ) {
      verificationContext.registerInternalApiUsage(KtInternalMethodUsage(methodReference, apiElement, usageLocation))
    }

    override fun registerField(
      fieldReference: FieldReference,
      apiElement: FieldLocation,
      usageLocation: MethodLocation
    ) {
      verificationContext.registerInternalApiUsage(KtInternalFieldUsage(fieldReference, apiElement, usageLocation))
    }
  }

}

