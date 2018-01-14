package com.jetbrains.plugin.structure.ide.util

import org.jetbrains.jps.model.JpsElementFactory
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.serialization.JpsProjectLoader

fun loadProject(projectPath: String, pathVariables: Map<String, String>): JpsProject {
  setDummyIdeaHomePath(projectPath)
  val model = JpsElementFactory.getInstance().createModel()
  JpsProjectLoader.loadProject(model.project, pathVariables, projectPath)
  return model.project
}

private fun setDummyIdeaHomePath(projectPath: String) {
  //It must be set to avoid initialization exceptions from com.intellij.openapi.application.PathManager.getHomePath()
  System.setProperty("idea.home.path", projectPath)
}