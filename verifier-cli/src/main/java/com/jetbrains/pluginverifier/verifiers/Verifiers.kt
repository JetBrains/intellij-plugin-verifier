package com.jetbrains.pluginverifier.verifiers

import com.google.common.base.Joiner
import com.google.common.collect.HashMultimap
import com.google.common.collect.Lists
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VOptions
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.CyclicDependenciesProblem
import com.jetbrains.pluginverifier.problems.MissingDependencyProblem
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.utils.Util
import com.jetbrains.pluginverifier.utils.dependencies.Dependencies
import com.jetbrains.pluginverifier.utils.dependencies.PluginDependenciesNode
import com.jetbrains.pluginverifier.verifiers.clazz.AbstractMethodVerifier
import com.jetbrains.pluginverifier.verifiers.clazz.InterfacesVerifier
import com.jetbrains.pluginverifier.verifiers.clazz.SuperClassVerifier
import com.jetbrains.pluginverifier.verifiers.field.FieldTypeVerifier
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier
import com.jetbrains.pluginverifier.verifiers.instruction.*
import com.jetbrains.pluginverifier.verifiers.method.*
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.slf4j.LoggerFactory
import java.util.*

private val LOG = LoggerFactory.getLogger(Verifiers::class.java)

object Verifiers {

  //TODO: add a verifier which reports minor problems (missing optional plugin descriptor, missing logo file and other)

  val PLUGIN_VERIFIERS = arrayOf<Verifier>(ReferencesVerifier())

  val fieldVerifiers = arrayOf<FieldVerifier>(FieldTypeVerifier())

  val classVerifiers = arrayOf(
      SuperClassVerifier(),
      InterfacesVerifier(),
      AbstractMethodVerifier()
  )
  val methodVerifiers = arrayOf(
      OverrideNonFinalVerifier(),
      MethodReturnTypeVerifier(),
      MethodArgumentTypesVerifier(),
      MethodLocalVarsVerifier(),
      MethodThrowsVerifier(),
      MethodTryCatchVerifier()
  )

  val instructionVerifiers = arrayOf(
      InvokeInstructionVerifier(),
      TypeInstructionVerifier(),
      LdcInstructionVerifier(),
      MultiANewArrayInstructionVerifier(),
      FieldAccessInstructionVerifier(),
      InvokeDynamicVerifier()
  )

  @Throws(InterruptedException::class)
  fun processAllVerifiers(ctx: VContext): VResult {
    //collect the problems in the context
    for (verifier in PLUGIN_VERIFIERS) {
      verifier.verify(ctx)
    }

    if (ctx.problems.isEmpty) {
      return VResult.Nice(ctx.pluginDescriptor, ctx.ideDescriptor, ctx.overview)
    } else {
      return VResult.Problems(ctx.pluginDescriptor, ctx.ideDescriptor, ctx.overview, ctx.problems)
    }
  }
}

private class ReferencesVerifier : Verifier {

  /**
   * @throws InterruptedException if the verification was cancelled
   */
  @Throws(InterruptedException::class)
  override fun verify(ctx: VContext) {
    val plugin = ctx.plugin

    val dependencies = Dependencies.getInstance().calcDependencies(ctx.plugin, ctx.ide)
    if (dependencies.cycle != null && Util.failOnCyclicDependency()) {
      val cycle = Joiner.on(" -> ").join(dependencies.cycle!!)
      ctx.registerProblem(CyclicDependenciesProblem(cycle), ProblemLocation.fromPlugin(plugin.pluginId))
      LOG.error("The plugin verifier will not verify a plugin $plugin because its dependencies tree has the following cycle: $cycle")
      return
    }
    val depNode = dependencies.descriptor

    for (entry in depNode.missingDependencies.entries) {
      val missingId = entry.key.id
      if (!ctx.verifierOptions.isIgnoreDependency(missingId)) {
        ctx.registerProblem(MissingDependencyProblem(plugin.pluginId, missingId, entry.value.reason), ProblemLocation.fromPlugin(plugin.pluginId))
      }
    }

    val missingMandatoryDeps = depNode.missingDependencies.keys.filter { !it.isOptional }.toList()
    if (!missingMandatoryDeps.isEmpty()) {
      LOG.error("The plugin verifier will not verify a plugin $plugin because it has missing mandatory dependencies: $missingMandatoryDeps")
      return
    }

    val mandatoryDeps: MutableSet<PluginDependenciesNode> = hashSetOf()
    dfsMandatoryDependencies(depNode, mandatoryDeps)

    createPluginDependenciesResolver(depNode, mandatoryDeps).use { dependenciesResolver ->
      val resolverForCheck = getResolverForCheck(plugin, ctx.pluginResolver)

      //Don't close this resolver because it contains IDE and JDK resolvers which are not ready to be closed. They will be closed by the caller.
      val pluginClassLoader = createPluginClassLoader(dependenciesResolver, ctx)

      for (className in resolverForCheck.allClasses) {
        if (Thread.currentThread().isInterrupted) {
          throw InterruptedException("The verification was cancelled")
        }

        val node = VerifierUtil.findClass(resolverForCheck, className, ctx)

        if (node != null) {
          verifyClass(pluginClassLoader, node, ctx)
        }
      }
    }

  }

  /**
   * Traverse the dependencies starting from [node]. Only the mandatory (non-optional) dependencies edges are considered.
   */
  private fun dfsMandatoryDependencies(node: PluginDependenciesNode, accumulator: MutableSet<PluginDependenciesNode>) {
    accumulator.add(node)

    val directMandatoryDeps = listOf(*node.plugin.dependencies.toTypedArray(), *node.plugin.moduleDependencies.toTypedArray()).filter { !it.isOptional }

    for (to in node.edges) {
      if (directMandatoryDeps.find { it.id.equals(to.plugin.pluginId) } != null) {
        if (!accumulator.contains(to)) {
          dfsMandatoryDependencies(to, accumulator)
        }
      }
    }

  }

  private fun createPluginDependenciesResolver(depNode: PluginDependenciesNode,
                                               mandatoryDependencies: MutableSet<PluginDependenciesNode>): Resolver {
    if (depNode.transitiveDependencies.isEmpty()) {
      return Resolver.getEmptyResolver()
    }
    val depResolvers = ArrayList<Resolver>()
    for (dep in depNode.transitiveDependencies) {
      try {
        depResolvers.add(Resolver.createPluginResolver(dep))
      } catch (e: Exception) {
        val isMandatory = mandatoryDependencies.find { it.plugin.equals(dep) } != null
        val message = "Unable to read class-files of the ${if (isMandatory) "mandatory" else "optional"} plugin ${dep.pluginId}"

        if (isMandatory) {
          LOG.error("The plugin verifier will not verify a plugin because its dependent plugin $dep has broken class-files", e)
          depResolvers.forEach({ it.close() })
          throw RuntimeException(message, e)
        } else {
          //non-mandatory plugins are not important.
          LOG.error(message, e)
        }
      }
    }
    return Resolver.createUnionResolver("Plugin " + depNode.plugin + " transitive dependencies", depResolvers)
  }

  /**
   * Constructs the plugin class-loader class-path.
   *
   * We use the following sequence for searching the class
   * 1. plugin itself (classes and libs)
   * 2. JDK classes
   * 3. IDE /lib classes
   * 4. plugin dependencies
   *
   * @param dependenciesResolver dependenciesResolver
   * @param context verification context
   * @return resolver
   */
  private fun createPluginClassLoader(dependenciesResolver: Resolver,
                                      context: VContext): Resolver {
    val plugin = context.plugin

    val list = ArrayList<Resolver>()

    list.add(context.pluginResolver)
    list.add(context.jdkResolver)
    list.add(context.ideResolver)
    list.add(dependenciesResolver)
    list.add(context.externalClassPath)

    val presentableName = "Common resolver for plugin " + plugin.pluginId + " with transitive dependencies; ide " + context.ide.version + "; jdk " + context.jdkResolver
    return Resolver.createCacheResolver(Resolver.createUnionResolver(presentableName, list))
  }

  /**
   * The Resolver for check consists of the jar-s which contain the classes referenced from META-INF/plugin.xml and related configuration files.
   * It's so that not to check classes not related to plugin (utility classes, libraries, etc)
   */
  private fun getResolverForCheck(plugin: Plugin, pluginResolver: Resolver): Resolver {
    val usedResolvers = Sets.newIdentityHashSet<Resolver>()

    val referencedFromXml = HashSet(plugin.allClassesReferencedFromXml)
    plugin.optionalDescriptors.values.forEach { x -> referencedFromXml.addAll(x.allClassesReferencedFromXml) }

    for (aClass in referencedFromXml) {
      val location = pluginResolver.getClassLocation(aClass)
      if (location != null) {
        usedResolvers.add(location)
      }
    }

    if (usedResolvers.isEmpty()) {
      return pluginResolver
    }

    return Resolver.createUnionResolver("Plugin classes for check", Lists.newArrayList(usedResolvers))
  }

  @SuppressWarnings("unchecked")
  private fun verifyClass(resolver: Resolver, node: ClassNode, ctx: VContext) {
    for (verifier in Verifiers.classVerifiers) {
      verifier.verify(node, resolver, ctx)
    }

    @Suppress("UNCHECKED_CAST")
    val methods = node.methods as List<MethodNode>
    for (method in methods) {
      for (verifier in Verifiers.methodVerifiers) {
        verifier.verify(node, method, resolver, ctx)
      }

      val instructions = method.instructions
      val i = instructions.iterator()
      while (i.hasNext()) {
        val instruction = i.next()
        for (verifier in Verifiers.instructionVerifiers) {
          verifier.verify(node, method, instruction as AbstractInsnNode, resolver, ctx)
        }
      }
    }

    @Suppress("UNCHECKED_CAST")
    val fields = node.fields as List<FieldNode>
    for (field in fields) {
      for (verifier in Verifiers.fieldVerifiers) {
        verifier.verify(node, field, resolver, ctx)
      }
    }
  }

}

class VContext(
    val plugin: Plugin,
    val pluginResolver: Resolver,
    val pluginDescriptor: PluginDescriptor = PluginDescriptor.ByInstance(plugin),
    val ide: Ide,
    val ideResolver: Resolver = Resolver.createIdeResolver(ide),
    val ideDescriptor: IdeDescriptor = IdeDescriptor.ByInstance(ide),
    val jdkResolver: Resolver,
    val verifierOptions: VOptions,
    val externalClassPath: Resolver = Resolver.getEmptyResolver()
) {
  val problems: Multimap<Problem, ProblemLocation> = HashMultimap.create<Problem, ProblemLocation>()
  var overview: String = ""

  fun registerProblem(problem: Problem, location: ProblemLocation) {
    if (!verifierOptions.isIgnoredProblem(plugin, problem)) {
      problems.put(problem, location)
    }
  }

}

interface Verifier {
  fun verify(ctx: VContext)
}