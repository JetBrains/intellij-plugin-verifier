package org.jetbrains.plugins.verifier.service

import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.persistence.GsonHolder
import grails.converters.JSON
import kotlin.text.StringsKt
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.jetbrains.plugins.verifier.service.util.LanguageUtilsKt
import org.springframework.http.HttpStatus

class IdeController {

  def list() {
    sendJson(IdeFilesManager.INSTANCE.ideList())
  }

  def upload() {
    def ideFile = params.ideFile
    if (!ideFile || ideFile.empty) {
      log.error("user attempted to upload empty IDE")
      response.sendError(HttpStatus.BAD_REQUEST.value(), "IDE file is empty")
      return
    }
    if (!ideFile.getOriginalFilename().endsWith(".zip")) {
      log.error("user attempted to upload non-.zip IDE")
      response.sendError(HttpStatus.BAD_REQUEST.value(), "IDE file should be a .zip archive")
      return
    }
    File tempFile = FileManager.INSTANCE.createTempFile(ideFile.getOriginalFilename() as String)
    try {
      try {
        ideFile.transferTo(tempFile)
      } catch (Exception e) {
        log.error("unable to save IDE", e)
        LanguageUtilsKt.deleteLogged(tempFile)
        response.sendError(HttpStatus.BAD_REQUEST.value(), "IDE is invalid")
        return
      }
      try {
        boolean success = IdeFilesManager.INSTANCE.addIde(tempFile)
        render([success: success] as JSON)
      } catch (Exception e) {
        log.error("unable to add the IDE", e)
        response.sendError(HttpStatus.BAD_REQUEST.value(), "IDE is invalid")
      }
    } finally {
      LanguageUtilsKt.deleteLogged(tempFile)
    }

  }

  def delete(String version) {
    if (StringsKt.isNullOrBlank(version)) {
      response.sendError(HttpStatus.BAD_REQUEST.value(), "Version $version is empty")
      return
    }
    IdeVersion ideVersion
    try {
      ideVersion = IdeVersion.createIdeVersion(version)
    } catch (Exception e) {
      response.sendError(HttpStatus.BAD_REQUEST.value(), "Version $version is incorrect: ${e.message}")
      return
    }
    IdeFilesManager.INSTANCE.deleteIde(ideVersion)
    render([success: true] as JSON)
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
