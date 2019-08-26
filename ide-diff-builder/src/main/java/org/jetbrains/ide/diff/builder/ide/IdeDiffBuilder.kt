package org.jetbrains.ide.diff.builder.ide

import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.jdk.JdkDescriptorCreator
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.jetbrains.ide.diff.builder.api.*
import org.jetbrains.ide.diff.builder.filter.ClassFilter
import java.nio.file.Path

/**
 * Builder of [ApiReport] by APIs difference of two IDEs.
 */
class IdeDiffBuilder(private val classFilter: ClassFilter, private val jdkPath: Path) {

  companion object {
    /**
     * com/example/Foo -> null
     * com/example/Some$Foo -> com/example/Some
     */
    fun getOuterClassName(className: String): String? {
      val packageName = className.substringBeforeLast("/")
      val simpleName = className.substringAfterLast("/")
      if ('$' in simpleName) {
        val outerSimpleName = simpleName.substringBeforeLast('$')
        return if (packageName.isEmpty()) {
          outerSimpleName
        } else {
          "$packageName/$outerSimpleName"
        }
      }
      return null
    }
  }

  fun buildIdeDiff(oldIdePath: Path, newIdePath: Path): ApiReport {
    val oldIde = IdeManager.createManager().createIde(oldIdePath.toFile())
    val newIde = IdeManager.createManager().createIde(newIdePath.toFile())
    return buildIdeDiff(oldIde, newIde)
  }


  fun buildIdeDiff(oldIde: Ide, newIde: Ide): ApiReport =
      JdkDescriptorCreator.createJdkDescriptor(jdkPath, Resolver.ReadMode.SIGNATURES).use { jdkDescriptor ->
        buildIdeResources(oldIde, Resolver.ReadMode.SIGNATURES).use { oldResources ->
          buildIdeResources(newIde, Resolver.ReadMode.SIGNATURES).use { newResources ->
            val completeOldResolver = CacheResolver(CompositeResolver.create(listOf(oldResources.allResolver, jdkDescriptor.jdkResolver)))
            val completeNewResolver = CacheResolver(CompositeResolver.create(listOf(newResources.allResolver, jdkDescriptor.jdkResolver)))

            val introducedProcessor = IntroducedProcessor()
            val removedProcessor = RemovedProcessor()
            val experimentalProcessor = ExperimentalProcessor()
            val deprecatedProcessor = DeprecatedProcessor()
            val diffBuilder = ApiDiffBuilder(
                classFilter,
                listOf(removedProcessor, introducedProcessor, experimentalProcessor, deprecatedProcessor)
            )

            diffBuilder.buildDiff(
                completeOldResolver,
                completeNewResolver,
                oldResources.allResolver.allClasses,
                newResources.allResolver.allClasses
            )

            buildApiReport(
                newIde.version,
                introducedProcessor,
                removedProcessor,
                experimentalProcessor,
                deprecatedProcessor
            )
          }
        }
      }

  @Suppress("DuplicatedCode")
  private fun buildApiReport(
      ideVersion: IdeVersion,
      introducedProcessor: IntroducedProcessor,
      removedProcessor: RemovedProcessor,
      experimentalProcessor: ExperimentalProcessor,
      deprecatedProcessor: DeprecatedProcessor
  ): ApiReport {
    val apiSignatureToEvents = hashMapOf<ApiSignature, MutableSet<ApiEvent>>()

    val removedIn = RemovedIn(ideVersion)
    for (signature in getNonNestedApiSignatures(removedProcessor.result)) {
      apiSignatureToEvents.getOrPut(signature) { hashSetOf() } += removedIn
    }

    val introducedIn = IntroducedIn(ideVersion)
    for (signature in getNonNestedApiSignatures(introducedProcessor.result)) {
      apiSignatureToEvents.getOrPut(signature) { hashSetOf() } += introducedIn
    }

    val markedExperimentalIn = MarkedExperimentalIn(ideVersion)
    for (signature in getNonNestedApiSignatures(experimentalProcessor.markedExperimental)) {
      apiSignatureToEvents.getOrPut(signature) { hashSetOf() } += markedExperimentalIn
    }

    val unmarkedExperimentalIn = UnmarkedExperimentalIn(ideVersion)
    for (signature in getNonNestedApiSignatures(experimentalProcessor.unmarkedExperimental)) {
      apiSignatureToEvents.getOrPut(signature) { hashSetOf() } += unmarkedExperimentalIn
    }

    val nonNestedDeprecated = getNonNestedApiSignatures(deprecatedProcessor.markedDeprecated.map { it.member })
    for ((member, forRemoval, inVersion) in deprecatedProcessor.markedDeprecated) {
      val signature = member.toSignature()
      if (signature in nonNestedDeprecated) {
        apiSignatureToEvents.getOrPut(signature) { hashSetOf() } += MarkedDeprecatedIn(ideVersion, forRemoval, inVersion)
      }
    }

    val unmarkedDeprecated = UnmarkedDeprecatedIn(ideVersion)
    for (signature in getNonNestedApiSignatures(deprecatedProcessor.unmarkedDeprecated)) {
      apiSignatureToEvents.getOrPut(signature) { hashSetOf() } += unmarkedDeprecated
    }

    return ApiReport(ideVersion, apiSignatureToEvents)
  }

  private fun getNonNestedApiSignatures(allMembers: List<ClassFileMember>): Set<ApiSignature> {
    val allSignatures: MutableSet<ApiSignature> = allMembers.mapTo(hashSetOf()) { it.toSignature() }
    val nestedSignatures = arrayListOf<ApiSignature>()
    for (signature in allSignatures) {
      val hostSignature = signature.containingClassSignature
      if (hostSignature != null && hostSignature in allSignatures) {
        nestedSignatures += signature
      }
    }
    allSignatures.removeAll(nestedSignatures)
    return allSignatures
  }

  private val ApiSignature.containingClassSignature: ClassSignature?
    get() = when (this) {
      is ClassSignature -> getOuterClassName(className)
      is MethodSignature -> hostSignature.className
      is FieldSignature -> hostSignature.className
    }?.let { ClassSignature(it) }

}

fun ClassFileMember.toSignature(): ApiSignature = when (this) {
  is ClassFile -> ClassSignature(name)
  is Method -> MethodSignature(
      ClassSignature(containingClassFile.name),
      name,
      descriptor,
      signature
  )
  is Field -> FieldSignature(
      ClassSignature(containingClassFile.name),
      name
  )
  else -> throw IllegalArgumentException()
}
