package org.jetbrains.plugins.verifier.service

import com.jetbrains.pluginverifier.misc.LanguageUtilsKt
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.springframework.http.HttpStatus

/**
 * @author Sergey Patrikeev
 */
trait SaveFileTrait extends SendResponseTrait {
  File savePluginTemporarily(pluginFile) {
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

}
