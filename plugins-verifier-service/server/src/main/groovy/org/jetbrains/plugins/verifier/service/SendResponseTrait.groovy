package org.jetbrains.plugins.verifier.service

import com.jetbrains.pluginverifier.persistence.GsonHolder

/**
 * @author Sergey Patrikeev
 */
trait SendResponseTrait {

  def sendError(int statusCode, String msg) {
    render(status: statusCode, text: msg, encoding: 'utf-8', contentType: 'text/plain')
  }

  def sendJson(Object obj) {
    String json
    if (obj instanceof String) {
      json = obj as String
    } else {
      json = GsonHolder.GSON.toJson(obj)
    }
    render(contentType: 'text/json', encoding: 'utf-8', text: json)
  }


}

