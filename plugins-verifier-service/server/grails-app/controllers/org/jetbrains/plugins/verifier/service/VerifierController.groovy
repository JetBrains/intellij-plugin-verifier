package org.jetbrains.plugins.verifier.service

import com.google.gson.Gson
import com.jetbrains.pluginverifier.persistence.GsonHolder
import kotlin.text.StringsKt
import org.jetbrains.plugins.verifier.service.api.Result
import org.jetbrains.plugins.verifier.service.api.Status
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.jetbrains.plugins.verifier.service.core.TaskManager
import org.jetbrains.plugins.verifier.service.params.CheckIdeRunnerParams
import org.jetbrains.plugins.verifier.service.params.CheckPluginAgainstSinceUntilBuildsRunnerParams
import org.jetbrains.plugins.verifier.service.params.CheckPluginRunnerParams
import org.jetbrains.plugins.verifier.service.runners.CheckIdeRunner
import org.jetbrains.plugins.verifier.service.runners.CheckPlugin
import org.jetbrains.plugins.verifier.service.runners.CheckPluginAgainstSinceUntilBuildsRunner
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.util.LanguageUtilsKt
import org.jetbrains.plugins.verifier.service.util.UnarchiverUtilKt
import org.springframework.http.HttpStatus

class VerifierController {

  private static final Gson GSON = GsonHolder.INSTANCE.GSON

  private TaskId parseTaskId() {
    def taskId
    try {
      taskId = GSON.fromJson(params.taskId as String, TaskId.class)
    } catch (Exception e) {
      def msg = "Invalid taskId: $e.message"
      log.error(msg, e)
      sendError(HttpStatus.BAD_REQUEST.value(), msg)
      return null
    }
    if (!taskId) {
      log.error("Null task ID")
      sendError(HttpStatus.BAD_REQUEST.value(), "Invalid taskId: null")
      return null
    }
    return taskId
  }

  def taskStatus() {
    def taskId = parseTaskId()
    if (!taskId) return

    Result<?> result = TaskManager.INSTANCE.get(taskId)
    if (!result) {
      def msg = "The task with such ID $taskId is not found"
      sendError(HttpStatus.NOT_FOUND.value(), msg)
      log.error(msg)
      return
    }
    def resultJson = GSON.toJson(result)
    log.info("Task ${taskId} result: $resultJson")
    sendJson(resultJson)
  }

  def printTaskResult(int id) {
    Result<?> result = TaskManager.INSTANCE.get(new TaskId(id))
    if (!result) {
      sendError(HttpStatus.NOT_FOUND.value(), "The task with such ID $id is not found")
      return
    }

    if (result.status == Status.WAITING) {
      render("Not started yet")
    } else if (result.status == Status.RUNNING) {
      render("Running now")
    } else if (result.status == Status.COMPLETE) {
      render result.result.toString()
    }
  }

  def cancelTask() {
    log.info("Task cancellation request $params")
    def taskId = parseTaskId()
    if (!taskId) return

    def canceled = TaskManager.INSTANCE.cancel(taskId)
    log.info("The task $taskId was canceled: $canceled")
    sendJson(canceled)
  }

  def checkPlugin() {
    def runnerParams = GSON.fromJson(params.params as String, CheckPluginRunnerParams.class)
    log.info("Runner params: $runnerParams")

    def pluginFiles = new ArrayList<File>()
    def ideFiles = new ArrayList<File>()

    try {
      params.findAll { (it.key as String).startsWith("plugin_") }.each {
        File file = savePluginTemporarily(it.value)
        if (file == null) {
          pluginFiles.forEach { LanguageUtilsKt.deleteLogged(it) }
          return
        } else {
          pluginFiles.add(file)
        }
      }

      params.findAll { (it.key as String).startsWith("ide_") }.each {
        File file = saveIdeTemporarily(it.value)
        if (file == null) {
          pluginFiles.forEach { LanguageUtilsKt.deleteLogged(it) }
          ideFiles.forEach { LanguageUtilsKt.deleteLogged(it) }
          return
        } else {
          ideFiles.add(file)
        }
      }

      def runner = new CheckPlugin(runnerParams, ideFiles, pluginFiles)
      def taskId = TaskManager.INSTANCE.enqueue(runner)
      sendJson(taskId)

    } catch (Exception e) {
      log.error("Unable to save the ide and plugins files", e)
      pluginFiles.each { LanguageUtilsKt.deleteLogged(it) }
      ideFiles.each { LanguageUtilsKt.deleteLogged(it) }
    }
  }

  def checkIde() {
    def saved = saveIdeTemporarily(params.ideFile)
    if (!saved) return
    def params = GSON.fromJson(params.params as String, CheckIdeRunnerParams.class)
    def runner = new CheckIdeRunner(saved, true, params)
    def taskId = TaskManager.INSTANCE.enqueue(runner)
    sendJson(taskId)
  }

  private def File saveIdeTemporarily(ideFile) {
    if (!ideFile || ideFile.empty) {
      log.error("user attempted to load empty IDE file")
      sendError(HttpStatus.BAD_REQUEST.value(), "IDE file is empty")
      return null
    }
    if (!ideFile.getOriginalFilename().endsWith(".zip")) {
      log.error("user attempted to load non-.zip archive IDE")
      sendError(HttpStatus.BAD_REQUEST.value(), "Only the .zip archived IDE-s are supported")
      return null
    }

    File tmpIdeFile
    try {
      tmpIdeFile = FileManager.INSTANCE.createTempFile(ideFile.getOriginalFilename() as String)
    } catch (Exception e) {
      log.error("Unable to create temp file for IDE", e)
      return null
    }

    try {
      ideFile.transferTo(tmpIdeFile)
    } catch (Exception e) {
      log.error("Unable to save IDE file $ideFile", e)
      LanguageUtilsKt.deleteLogged(tmpIdeFile)

      sendError(HttpStatus.BAD_REQUEST.value(), "The IDE file is invalid ${e.message}")
      return null
    }

    try {
      def ideFileName = StringsKt.substringBeforeLast(ideFile.getOriginalFilename() as String, ".", ideFile.getOriginalFilename() as String)
      def tempDirectory = FileManager.INSTANCE.createTempDirectory(ideFileName)
      def savedIde = UnarchiverUtilKt.extractTo(tmpIdeFile, tempDirectory)
      log.info("ide file saved to ${savedIde}")
      return savedIde
    } catch (Exception e) {
      log.debug("Unable to extract IDE file $tmpIdeFile", e)
      sendError(HttpStatus.BAD_REQUEST.value(), "The IDE file is invalid ${e.message}")
      return null
    } finally {
      LanguageUtilsKt.deleteLogged(tmpIdeFile)
    }
  }

  def checkPluginAgainstSinceUntilBuilds() {
    File saved = savePluginTemporarily(params.pluginFile)
    if (!saved) return
    def params = GSON.fromJson(params.params as String, CheckPluginAgainstSinceUntilBuildsRunnerParams.class)
    def runner = new CheckPluginAgainstSinceUntilBuildsRunner(saved, true, params)
    def taskId = TaskManager.INSTANCE.enqueue(runner)
    sendJson(taskId)
  }

  private File savePluginTemporarily(pluginFile) {
    if (!pluginFile || pluginFile.empty) {
      log.error("user attempted to load empty plugin file")
      sendError(HttpStatus.BAD_REQUEST.value(), "Empty plugin file")
      return null
    }

    File tmpFile = FileManager.INSTANCE.createTempFile((pluginFile.getOriginalFilename() as String) + ".zip")
    try {
      pluginFile.transferTo(tmpFile)
    } catch (Exception e) {
      log.error("Unable to save plugin file to $tmpFile", e)
      sendError(HttpStatus.BAD_REQUEST.value(), "The plugin file is broken")
      LanguageUtilsKt.deleteLogged(tmpFile)
      return null
    }

    log.info("plugin file saved to ${tmpFile}")
    return tmpFile
  }

  private sendError(int statusCode, String msg) {
    render(status: statusCode, text: msg, encoding: 'utf-8', contentType: 'text/plain')
  }

  private sendJson(Object obj) {
    String json
    if (obj instanceof String) {
      json = obj as String
    } else {
      json = GsonHolder.GSON.toJson(obj)
    }
    render(contentType: 'text/json', encoding: 'utf-8', text: json)
  }

  def index() {}

}
