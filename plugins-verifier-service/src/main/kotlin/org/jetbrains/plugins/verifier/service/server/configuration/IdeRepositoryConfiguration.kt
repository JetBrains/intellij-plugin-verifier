/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.server.configuration

import com.jetbrains.pluginverifier.ide.repositories.AndroidStudioIdeRepository
import com.jetbrains.pluginverifier.ide.repositories.CompositeIdeRepository
import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import com.jetbrains.pluginverifier.ide.repositories.ReleaseIdeRepository
import org.jetbrains.plugins.verifier.service.service.ide.AppCodeIdeRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IdeRepositoryConfiguration {

  private val log = LoggerFactory.getLogger(IdeRepositoryConfiguration::class.java)

  @Bean
  fun ideRepository(
    @Value("\${verifier.service.app.code.ide.repository.build.server.url:}")
    buildServerUrl: String,
    @Value("\${verifier.service.app.code.ide.repository.configuration.ids:}")
    configurationIds: String,
    @Value("\${verifier.service.app.code.ide.repository.auth.token:}")
    authToken: String
  ): IdeRepository = CompositeIdeRepository(
    listOfNotNull(
      ReleaseIdeRepository(),
      AndroidStudioIdeRepository(),
      appCodeRepository(buildServerUrl, authToken, configurationIds.split(",").map { it.trim() }.filterNot { it.isEmpty() })
    )
  )

  private fun appCodeRepository(buildServerUrl: String, authToken: String, configurationIds: List<String>): IdeRepository? {
    if (buildServerUrl.isEmpty() || authToken.isEmpty() || configurationIds.isEmpty()) {
      log.info("Skipping initialization of the AppCode IDE repository because corresponding startup options are not set")
      return null
    }
    val repository = AppCodeIdeRepository(buildServerUrl, authToken, configurationIds)
    return try {
      repository.fetchIndex()
      log.info("Successfully added IDE repository $repository")
      repository
    } catch (e: Exception) {
      log.warn("Unable to get index from AppCode IDE repository because it is not available (see the cause). $repository", e)
      null
    }
  }
}