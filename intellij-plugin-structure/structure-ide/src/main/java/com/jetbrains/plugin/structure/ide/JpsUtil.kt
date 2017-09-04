package com.jetbrains.plugin.structure.ide

import com.intellij.util.ConcurrencyUtil
import org.jetbrains.jps.model.JpsElementFactory
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.serialization.JpsProjectLoader
import org.jetbrains.jps.service.SharedThreadPool
import org.jetbrains.jps.service.impl.SharedThreadPoolImpl
import java.util.concurrent.ThreadPoolExecutor

/**
 * @author Sergey Patrikeev
 */
fun loadProject(projectPath: String, pathVariables: Map<String, String>): JpsProject {
  setDaemonForSharedThreadPool()
  setDummyIdeaHomePath(projectPath)
  val model = JpsElementFactory.getInstance().createModel()
  JpsProjectLoader.loadProject(model.project, pathVariables, projectPath)
  return model.project
}

private fun setDummyIdeaHomePath(projectPath: String) {
  //It must be set to avoid initialization exceptions from com.intellij.openapi.application.PathManager.getHomePath()
  System.setProperty("idea.home.path", projectPath)
}

/*
TODO: get rid of this when new IDEA with fixed initializer of org.jetbrains.jps.service.impl.SharedThreadPoolImpl.myService is uploaded to https://www.jetbrains.com/intellij-repository/releases/
 */
private fun setDaemonForSharedThreadPool() {
  val poolImpl = SharedThreadPool.getInstance() as SharedThreadPoolImpl
  val myServiceField = SharedThreadPoolImpl::class.java.getDeclaredField("myService")
  myServiceField.isAccessible = true
  val myService = myServiceField.get(poolImpl) as ThreadPoolExecutor
  myService.threadFactory = ConcurrencyUtil.newNamedThreadFactory("JPS thread pool", true, Thread.NORM_PRIORITY)
}
