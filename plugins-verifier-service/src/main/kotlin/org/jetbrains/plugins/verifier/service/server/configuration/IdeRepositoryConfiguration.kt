/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.server.configuration

import com.jetbrains.pluginverifier.ide.repositories.AndroidStudioIdeRepository
import com.jetbrains.pluginverifier.ide.repositories.CompositeIdeRepository
import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import com.jetbrains.pluginverifier.ide.repositories.ReleaseIdeRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IdeRepositoryConfiguration {
  @Bean
  fun ideRepository(): IdeRepository = CompositeIdeRepository(listOf(ReleaseIdeRepository(), AndroidStudioIdeRepository()))
}