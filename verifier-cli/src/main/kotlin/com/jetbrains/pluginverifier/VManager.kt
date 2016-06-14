package com.jetbrains.pluginverifier

import com.google.common.collect.HashMultimap
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.verifiers.VerificationContextImpl
import com.jetbrains.pluginverifier.verifiers.Verifiers
import org.slf4j.LoggerFactory


/**
 * @author Sergey Patrikeev
 */
object VManager {

  private val LOG = LoggerFactory.getLogger(VManager::class.java)

  fun verify(params: VParams): VResults {

    LOG.debug("Verifying $params")

    val results = arrayListOf<PluginOnIdeResult>()

    //IOException is acceptable
    Resolver.createJdkResolver(params.runtimeDir).use { runtimeResolver ->

      //Group by IDE to reduce the Ide-Resolver creations number.
      params.pluginsToCheck.groupBy { it.ide }.entries.forEach { ideToPlugins ->

        LOG.debug("Creating Resolver for ${ideToPlugins.key}")

        val ideResolver: Resolver
        try {
          ideResolver = Resolver.createIdeResolver(ideToPlugins.key)
        } catch (e: Exception) {
          val message = "Failed to create Resolver for ${ideToPlugins.key}"
          LOG.error(message, e)
          ideToPlugins.value.forEach { results.add(PluginOnIdeResult.Error(it, message, e)) }
          return@forEach
        }

        ideResolver.use { ideResolver ->
          ideToPlugins.value.forEach poi@ { pluginOnIde ->

            LOG.debug("Verifying ${pluginOnIde.plugin} against ${pluginOnIde.ide}")

            val ctx = VerificationContextImpl(pluginOnIde.plugin, pluginOnIde.ide, ideResolver, runtimeResolver, params.externalClassPath, params.options)

            try {
              Verifiers.processAllVerifiers(ctx)
            } catch (e: Exception) {
              val message = "Failed to verify ${pluginOnIde.plugin} against ${pluginOnIde.ide}"
              LOG.error(message, e)
              results.add(PluginOnIdeResult.Error(pluginOnIde, message, e))
              return@poi
            }

            val problems = HashMultimap.create<Problem, ProblemLocation>()

            ctx.problemSet.asMap().forEach { entry -> entry.value.forEach { location -> problems.put(entry.key, location) } }

            results.add(PluginOnIdeResult.Success(pluginOnIde, ctx.overview, problems))

            LOG.debug("Successfully verified ${pluginOnIde.plugin} against ${pluginOnIde.ide}")
          }
        }
      }
    }

    return VResults(results)
  }

}