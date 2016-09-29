package org.jetbrains.plugins.verifier.service

import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.misc.LanguageUtilsKt
import com.jetbrains.pluginverifier.persistence.GsonHolder
import grails.converters.JSON
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.storage.ReportsManager
import org.springframework.http.HttpStatus

class ReportsController {

  def index() {}

  def list() {
    sendJson(ReportsManager.INSTANCE.listReports())
  }

  def upload() {
    def reportFile = params.reportFile
    if (!reportFile || reportFile.empty) {
      log.error("user attempted to load empty report")
      sendError(HttpStatus.BAD_REQUEST.value(), "Report file is empty")
      return
    }
    File tempFile = FileManager.INSTANCE.createTempFile("report")
    try {
      try {
        reportFile.transferTo(tempFile)
      } catch (Exception e) {
        log.error("unable to save the report", e)
        LanguageUtilsKt.deleteLogged(tempFile)
        sendError(HttpStatus.BAD_REQUEST.value(), "Report file is invalid")
        return
      }
      try {
        boolean success = ReportsManager.INSTANCE.saveReport(tempFile)
        render([success: success] as JSON)
      } catch (Exception e) {
        log.error("unable to save the report", e)
        sendError(HttpStatus.BAD_REQUEST.value(), "Report file is invalid")
      }
    } finally {
      LanguageUtilsKt.deleteLogged(tempFile)
    }

  }

  def delete(String ideVersion) {
    IdeVersion version
    try {
      version = IdeVersion.createIdeVersion(ideVersion)
    } catch (Exception e) {
      log.error("user specified an invalid ideVersion: $ideVersion", e)
      sendError(HttpStatus.BAD_REQUEST.value(), "IDE version \'$ideVersion\' is invalid")
      return
    }
    def success = ReportsManager.INSTANCE.deleteReport(version)
    render([success: success] as JSON)
  }

  def get(String ideVersion) {
    IdeVersion version
    try {
      version = IdeVersion.createIdeVersion(ideVersion)
    } catch (Exception e) {
      log.error("user ideVersion is invalid: $ideVersion", e)
      sendError(HttpStatus.BAD_REQUEST.value(), "IDE version \'$ideVersion\' is invalid")
      return
    }
    File report = ReportsManager.INSTANCE.getReport(version)
    if (report != null) {
      response.setContentType("application/octet-stream")
      response.setHeader("Content-disposition", "attachment;filename=$report.name")
      report.withInputStream { response.outputStream << it }
    } else {
      sendError(HttpStatus.NOT_FOUND.value(), "No such report \'$ideVersion\' found on the server")
    }
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

}
