package com.jetbrains.plugin.structure.ide

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.SystemProperties
import com.jetbrains.plugin.structure.base.utils.isJar
import org.jetbrains.jps.model.JpsElementFactory
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.serialization.JpsProjectLoader
import java.io.File

fun getRepositoryLibrariesJars(projectPath: File): List<File> {
  val pathVariables = createPathVariables()
  val project = loadProject(projectPath.absoluteFile, pathVariables)
  return JpsJavaExtensionService.dependencies(project)
    .productionOnly()
    .runtimeOnly()
    .libraries
    .flatMap { it.getFiles(JpsOrderRootType.COMPILED) }
    .distinctBy { it.path }
    .filter { it.isJar() }
}

private fun loadProject(projectPath: File, pathVariables: Map<String, String>): JpsProject {
  //It must be set to avoid initialization exceptions from com.intellij.openapi.application.PathManager.getHomePath()
  System.setProperty("idea.home.path", projectPath.absolutePath)

  val model = JpsElementFactory.getInstance().createModel()
  JpsProjectLoader.loadProject(model.project, pathVariables, projectPath.absolutePath)
  return model.project
}

private fun createPathVariables(): Map<String, String> {
  val mavenRepoFile = System.getProperty("MAVEN_REPOSITORY")?.let { File(it) }
    ?: File(SystemProperties.getUserHome(), ".m2/repository")
  val m2Repo = FileUtil.toSystemIndependentName(mavenRepoFile.absolutePath)
  return mapOf("MAVEN_REPOSITORY" to m2Repo)
}