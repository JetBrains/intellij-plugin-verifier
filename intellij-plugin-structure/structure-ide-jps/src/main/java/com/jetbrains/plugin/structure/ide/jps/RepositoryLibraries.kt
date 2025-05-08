/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.jps

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.SystemProperties
import com.jetbrains.plugin.structure.base.utils.isJar
import org.jetbrains.jps.model.JpsElementFactory
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.serialization.JpsProjectLoader
import java.nio.file.Path
import java.nio.file.Paths

fun getRepositoryLibrariesJars(projectPath: Path): List<Path> {
  val pathVariables = createPathVariables()
  val project = loadProject(projectPath, pathVariables)
  return JpsJavaExtensionService.dependencies(project)
    .productionOnly()
    .runtimeOnly()
    .libraries
    .flatMap { it.getFiles(JpsOrderRootType.COMPILED) }
    .map { it.toPath() }
    .distinctBy { it.toString() }
    .filter { it.isJar() }
}

private fun loadProject(projectPath: Path, pathVariables: Map<String, String>): JpsProject {
  //It must be set to avoid initialization exceptions from com.intellij.openapi.application.PathManager.getHomePath()
  System.setProperty("idea.home.path", projectPath.toAbsolutePath().toString())

  val model = JpsElementFactory.getInstance().createModel()
  JpsProjectLoader.loadProject(model.project, pathVariables, projectPath.toAbsolutePath().toString())
  return model.project
}

private fun createPathVariables(): Map<String, String> {
  val mavenRepoFile = System.getProperty("MAVEN_REPOSITORY")?.let { Paths.get(it) }
    ?: Paths.get(SystemProperties.getUserHome()).resolve(".m2").resolve("repository")
  val m2Repo = FileUtil.toSystemIndependentName(mavenRepoFile.toAbsolutePath().toString())
  return mapOf("MAVEN_REPOSITORY" to m2Repo)
}