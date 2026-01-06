/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.jdk

import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.pluginverifier.jdk.JdkDescriptorProvider.Result.Found
import com.jetbrains.pluginverifier.jdk.JdkDescriptorProvider.Result.NotFound
import java.nio.file.Path
import java.nio.file.Paths

interface JdkDescriptorProvider {
  fun getJdkDescriptor(ide: Ide, defaultJdkPath: Path?): Result

  sealed class Result {
    data class Found(val jdkDescriptor: JdkDescriptor) : Result()
    object NotFound: Result()
  }
}

private fun JdkDescriptor?.toResult(): JdkDescriptorProvider.Result {
  return this?.let { Found(it) } ?: NotFound
}

class DefaultJdkDescriptorProvider: JdkDescriptorProvider {
  override fun getJdkDescriptor(ide: Ide, defaultJdkPath: Path?): JdkDescriptorProvider.Result {
    val jdkDescriptor =
      fromExplicitPath(defaultJdkPath)
      ?: fromIdeBundled(ide)
      ?: fromJavaHome()
      ?: fromCurrentJvm()
    return jdkDescriptor.toResult()
  }

  private fun fromIdeBundled(ide: Ide): JdkDescriptor? {
    return JdkDescriptorCreator.createBundledJdkDescriptor(ide)
  }

  private fun fromExplicitPath(javaHome: Path?): JdkDescriptor? {
    return if (javaHome?.isDirectory == true) {
      JdkDescriptorCreator.createJdkDescriptor(javaHome)
    } else {
      null
    }
  }

  private fun fromJavaHome(): JdkDescriptor? {
    val javaHome: Path? = try {
       System.getenv("JAVA_HOME")
    } catch (_: SecurityException) {
      null
    }?.let {
      Paths.get(it)
    }?.takeIf {
      it.isDirectory
    }

    return javaHome?.let {
      JdkDescriptorCreator.createJdkDescriptor(it)
    }
  }
  private fun fromCurrentJvm(): JdkDescriptor? {
    val javaHome: Path? = try {
       System.getProperty("java.home")
    } catch (_: SecurityException) {
      null
    }?.let {
      Paths.get(it)
    }?.takeIf {
      it.isDirectory
    }

    return javaHome?.let {
      JdkDescriptorCreator.createJdkDescriptor(it)
    }
  }
  private fun fromCurrentJvm(): JdkDescriptor? {
    val javaHome: Path? = try {
       System.getProperty("java.home")
    } catch (e: SecurityException) {
      null
    }?.let {
      Paths.get(it)
    }?.takeIf {
      it.isDirectory
    }

    return javaHome?.let {
      JdkDescriptorCreator.createJdkDescriptor(it)
    }
  }
}