package org.jetbrains.plugins.verifier.service

import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.persistence.GsonHolder
import grails.converters.JSON
import kotlin.text.StringsKt
import org.jetbrains.plugins.verifier.service.core.TaskManager
import org.jetbrains.plugins.verifier.service.runners.UploadIdeRunner
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.jetbrains.plugins.verifier.service.util.LanguageUtilsKt
import org.springframework.http.HttpStatus

class IdeController {

  private sendError(int statusCode, String msg) {
    render(status: statusCode, text: msg, encoding: 'utf-8', contentType: 'text/plain')
  }

  def list() {
    sendJson(IdeFilesManager.INSTANCE.ideList())
  }

  def uploadFromRepository() {
    def ideVersion = params.ideVersion as String
    if (!ideVersion) {
      sendError(HttpStatus.BAD_REQUEST.value(), "IDE version is empty")
      return
    }
    boolean fromSnapshots = params.snapshots != null
    boolean isCommunity = params.community != null

    log.info("User is going to UPLOAD the new IDE #$ideVersion ${if (isCommunity) "community" else ""} " +
        "from the ${if (fromSnapshots) "snapshots" else ""} repository")

    def runner = new UploadIdeRunner(ideVersion, isCommunity, fromSnapshots)
    def taskId = TaskManager.INSTANCE.enqueue(runner)
    log.info("Upload IDE #$ideVersion is enqueued with taskId=$taskId")
    sendJson(taskId)
  }

  def upload() {
    log.info("User is going to UPLOAD the new IDE file")
    def ideFile = params.ideFile
    if (!ideFile || ideFile.empty) {
      log.error("user attempted to upload empty IDE")
      sendError(HttpStatus.BAD_REQUEST.value(), "IDE file is empty")
      return
    }
    if (!ideFile.getOriginalFilename().endsWith(".zip")) {
      log.error("user attempted to upload non-.zip IDE")
      sendError(HttpStatus.BAD_REQUEST.value(), "IDE file should be a .zip archive")
      return
    }
    File tempFile = FileManager.INSTANCE.createTempFile(ideFile.getOriginalFilename() as String)
    try {
      try {
        log.debug("Temporarily save IDE to $tempFile")
        ideFile.transferTo(tempFile)
      } catch (Exception e) {
        log.error("unable to save IDE", e)
        LanguageUtilsKt.deleteLogged(tempFile)
        sendError(HttpStatus.BAD_REQUEST.value(), "IDE is invalid")
        return
      }
      try {
        boolean success = IdeFilesManager.INSTANCE.addIde(tempFile)
        render([success: success] as JSON)
        log.info("IDE file has been successfully uploaded. IDE list: ${IdeFilesManager.INSTANCE.ideList()}")
      } catch (Exception e) {
        log.error("unable to add the IDE", e)
        sendError(HttpStatus.BAD_REQUEST.value(), "IDE is invalid")
      }
    } finally {
      LanguageUtilsKt.deleteLogged(tempFile)
    }

  }

  def delete(String version) {
    log.info("User is going to DELETE the IDE file #$version")
    if (StringsKt.isNullOrBlank(version)) {
      sendError(HttpStatus.BAD_REQUEST.value(), "Version $version is empty")
      return
    }
    IdeVersion ideVersion
    try {
      ideVersion = IdeVersion.createIdeVersion(version)
    } catch (Exception e) {
      sendError(HttpStatus.BAD_REQUEST.value(), "Version $version is incorrect: ${e.message}")
      return
    }
    IdeFilesManager.INSTANCE.deleteIde(ideVersion)
    render([success: true] as JSON)
    log.info("IDE #$version has been successfully deleted")
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
