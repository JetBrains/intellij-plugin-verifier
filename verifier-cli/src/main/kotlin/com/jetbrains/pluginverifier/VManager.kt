package com.jetbrains.pluginverifier

import com.google.common.collect.HashMultimap
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.verifiers.VerificationContextImpl
import com.jetbrains.pluginverifier.verifiers.Verifiers
import org.slf4j.LoggerFactory
import java.io.IOException


/**
 * @author Sergey Patrikeev
 */
object VManager {

  private val LOG = LoggerFactory.getLogger(VManager::class.java)

  /**
   * Perform the verification according to passed parameters.
   *
   * Parameters consist of the _(ide, plugin)_ pairs.
   * Every plugin is checked against the corresponding IDE.
   * For every such pair the method returns a result of the verification:
   * normally every result consists of the found binary problems (the main task of the Verifier), if they exist,
   * but there could be the verification errors too (the most typical are due to broken plugin mandatory dependencies,
   * invalid plugin class-files or whatever other reasons including the bugs of the Verifier itself).
   * Thus you should check the type of the _(ide, plugin)_ pairs results (see {#VResults}) .
   *
   * @return the verification result
   * @throws IOException if io-errors occur. The other problems are hidden in the corresponding _(ide, plugin)_ (with the message and cause).
   */
  @Throws(IOException::class)
  fun verify(params: VParams): VResults {

    LOG.debug("Verifying $params")

    val results = arrayListOf<VResult>()

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
          ideToPlugins.value.forEach { results.add(VResult.VerificationError(it.pluginDescriptor, it.ideDescriptor, message, e)) }
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
              results.add(VResult.VerificationError(pluginOnIde.pluginDescriptor, pluginOnIde.ideDescriptor, message, e))
              return@poi
            }

            val problems = HashMultimap.create<Problem, ProblemLocation>()

            ctx.problemSet.asMap().forEach { entry -> entry.value.forEach { location -> problems.put(entry.key, location) } }

            results.add(VResult.Problems(pluginOnIde.pluginDescriptor, pluginOnIde.ideDescriptor, ctx.overview, problems))

            LOG.debug("Successfully verified ${pluginOnIde.plugin} against ${pluginOnIde.ide}")
          }
        }
      }
    }

    return VResults(results)
  }

}