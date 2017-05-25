package org.jetbrains.plugins.verifier.service

import com.google.gson.Gson
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.misc.LanguageUtilsKt
import com.jetbrains.pluginverifier.misc.UnarchiverUtilKt
import com.jetbrains.pluginverifier.repository.IdleFileLock
import kotlin.text.StringsKt
import org.jetbrains.plugins.verifier.service.api.Result
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.jetbrains.plugins.verifier.service.core.TaskManager
import org.jetbrains.plugins.verifier.service.params.CheckIdeRunnerParams
import org.jetbrains.plugins.verifier.service.params.CheckPluginRunnerParams
import org.jetbrains.plugins.verifier.service.params.CheckRangeRunnerParams
import org.jetbrains.plugins.verifier.service.params.CheckTrunkApiRunnerParams
import org.jetbrains.plugins.verifier.service.runners.CheckIdeRunner
import org.jetbrains.plugins.verifier.service.runners.CheckPlugin
import org.jetbrains.plugins.verifier.service.runners.CheckRangeRunner
import org.jetbrains.plugins.verifier.service.runners.CheckTrunkApiRunner
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.springframework.http.HttpStatus

class VerifierController implements SaveFileTrait {

  private static final Gson GSON = new Gson()

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
    if (log.debugEnabled) {
      log.debug("Task ${taskId} status: ${result.taskStatus}, content: ${result.result == null ? "<is not calculated yet>" : "[${result.result.toString().take(1000)}...]"}")
    }
    sendJson(resultJson)
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
      log.info("New Check-Plugin command is enqueued with taskId=$taskId")
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
    log.info("New Check-Ide command is enqueued with taskId=$taskId")
  }

  def checkTrunkApi() {
    def saved = saveIdeTemporarily(params.ideFile)
    if (!saved) return
    def params = GSON.fromJson(params.params as String, CheckTrunkApiRunnerParams.class)
    def runner = new CheckTrunkApiRunner(saved, true, params)
    def taskId = TaskManager.INSTANCE.enqueue(runner)
    sendJson(taskId)
    log.info("New Check-Trunk-Api command is enqueued with taskId=$taskId")
  }

  private File saveIdeTemporarily(ideFile) {
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
      sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server disk is exhausted")
      return null
    }

    try {
      ideFile.transferTo(tmpIdeFile)
    } catch (Exception e) {
      log.error("Unable to save IDE file $ideFile", e)
      LanguageUtilsKt.deleteLogged(tmpIdeFile)
      sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to save the IDE, probably no space left")
      return null
    }

    try {
      def ideFileName = StringsKt.substringBeforeLast(ideFile.getOriginalFilename() as String, ".", ideFile.getOriginalFilename() as String)
      def tempDirectory = FileManager.INSTANCE.createTempDirectory(ideFileName)
      def savedIde = UnarchiverUtilKt.extractTo(tmpIdeFile, tempDirectory)
      log.info("ide file saved to ${savedIde}")
      return savedIde
    } catch (Exception e) {
      log.error("Unable to extract IDE file $tmpIdeFile", e)
      sendError(HttpStatus.BAD_REQUEST.value(), "The IDE file is invalid ${e.message}")
      return null
    } finally {
      LanguageUtilsKt.deleteLogged(tmpIdeFile)
    }
  }

  def checkPluginRange() {
    File saved = savePluginTemporarily(params.pluginFile)
    if (!saved) return
    def runnerParams = GSON.fromJson(params.params as String, CheckRangeRunnerParams.class)
    def byFile = new PluginDescriptor.ByFileLock(new IdleFileLock(saved))
    def runner = new CheckRangeRunner(byFile, runnerParams, null)

    def onSuccess = { result -> return null }
    def onError = { one, two, three -> return null }
    def onCompletion = { one, two -> LanguageUtilsKt.deleteLogged(saved); return null }

    def taskId = TaskManager.INSTANCE.enqueue(runner, onSuccess, onError, onCompletion)

    sendJson(taskId)
    log.info("New Check-Plugin-With-[since;until] is enqueued with taskId=$taskId")
  }


  def index() {}

}
