package com.jetbrains.pluginverifier

import com.google.common.collect.HashMultimap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.Plugin
import com.intellij.structure.errors.IncorrectPluginException
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

  /**
   * Perform the verification according to passed parameters.
   *
   * The parameters consist of the _(ide, plugin)_ pairs.
   * Every plugin is checked against the corresponding IDE.
   * For every such pair the method returns a result of the verification:
   * normally every result consists of the found binary problems (the main task of the Verifier), if they exist.
   * But there could be the verification errors (the most typical are due to missing plugin mandatory dependencies,
   * invalid plugin class-files or whatever other reasons).
   * Thus you should check the type of the _(ide, plugin)_ pairs results (see [VResult]) .
   *
   * @return the verification results
   * @throws RuntimeException if unexpected errors occur: e.g. the IDE broken, or the Repository doesn't respond.
   */
  fun verify(params: VParams): VResults {

    LOG.info("Verifying the plugins according to $params")

    val results = arrayListOf<VResult>()

    //IOException is propagated.
    Resolver.createJdkResolver(params.runtimeDir).use { runtimeResolver ->

      //Group by IDE to reduce the Ide-Resolver creations number.
      params.pluginsToCheck.groupBy { it.second }.entries.forEach { ideToPlugins ->

        LOG.info("Creating Resolver for ${ideToPlugins.key}")

        val ide: Ide
        val ideResolver: Resolver
        try {
          ide = VParamsCreator.getIde(ideToPlugins.key)
          ideResolver = Resolver.createIdeResolver(ide)
        } catch(e: Exception) {
          //IDE errors are propagated.
          throw RuntimeException("Failed to create IDE instance for ${ideToPlugins.key}")
        }

        //auto-close
        ideResolver.use { ideResolver ->
          ideToPlugins.value.forEach poi@ { pluginOnIde ->

            LOG.info("Verifying ${pluginOnIde.first} against ${pluginOnIde.second}")

            val plugin: Plugin
            try {
              plugin = VParamsCreator.getPlugin(pluginOnIde.first, ide.version)
            } catch(e: IncorrectPluginException) {
              //the plugin has incorrect structure.
              results.add(VResult.BadPlugin(pluginOnIde.first, e.message ?: e.javaClass.name))
              return@poi
            } catch(e: Exception) {
              results.add(VResult.BadPlugin(pluginOnIde.first, e.message ?: e.javaClass.name))
              return@poi
            }

            val ctx = VerificationContextImpl(plugin, ide, ideResolver, runtimeResolver, params.externalClassPath, params.options)

            try {
              Verifiers.processAllVerifiers(ctx)
            } catch (e: Exception) {
              val message = "Failed to verify ${pluginOnIde.first} against ${pluginOnIde.second}"
              LOG.error(message, e)
              results.add(VResult.BadPlugin(pluginOnIde.first, message, e))
              return@poi
            }

            if (ctx.problemSet.isEmpty) {
              results.add(VResult.Nice(pluginOnIde.first, ctx.overview))
            } else {
              val problems = HashMultimap.create<Problem, ProblemLocation>()

              ctx.problemSet.asMap().forEach { entry -> entry.value.forEach { location -> problems.put(entry.key, location) } }

              results.add(VResult.Problems(pluginOnIde.first, pluginOnIde.second, ctx.overview, problems))
            }

            LOG.info("Successfully verified ${plugin.pluginFile} against ${pluginOnIde.second}")
          }
        }
      }
    }

    return VResults(results)
  }

}