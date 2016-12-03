package org.jetbrains.plugins.verifier.service

import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.misc.LanguageUtilsKt
import org.jetbrains.plugins.verifier.service.core.TaskManager
import org.jetbrains.plugins.verifier.service.runners.ExtractFeaturesRunner

class FeaturesController implements SaveFileTrait {

  def index() {

  }

  def extractPluginFeatures() {
    def saved = savePluginTemporarily(params.pluginFile)
    if (!saved) return
    def byFile = new PluginDescriptor.ByFile("id", "version", saved)
    def featuresRunner = new ExtractFeaturesRunner(byFile)
    def taskId = TaskManager.INSTANCE.enqueue(featuresRunner, { result -> }, { one, two, three -> }, { one, two -> LanguageUtilsKt.deleteLogged(saved) })
    sendJson(taskId)
  }
}