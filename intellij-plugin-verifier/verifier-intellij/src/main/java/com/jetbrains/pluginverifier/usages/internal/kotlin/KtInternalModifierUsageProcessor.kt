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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(KtInternalModifierUsageProcessor::class.java)

/**
 * ByteBuddy modifiers when using the filtering with delegation:
 * ```
 * MethodDelegation.withDefaultConfiguration().filter(...)
 * ```
 */
private val byteBuddyMethodDelegationModifiers = Modifiers.of(PUBLIC, STATIC, VOLATILE, BRIDGE, SYNTHETIC)

class KtInternalModifierUsageProcessor(verificationContext: PluginVerificationContext) : BaseInternalApiUsageProcessor(KtInternalUsageRegistrar(verificationContext))  {
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
    if (isIgnored(resolvedMember, usageLocation)) {
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

  private fun isIgnored(member: ClassFileMember, usageLocation: Location): Boolean {
    return usageLocation is FieldLocation && usageLocation.modifiers == byteBuddyMethodDelegationModifiers
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
      LOG.atInfo().log("Registering field: {}", fieldReference)
    }
  }

}

