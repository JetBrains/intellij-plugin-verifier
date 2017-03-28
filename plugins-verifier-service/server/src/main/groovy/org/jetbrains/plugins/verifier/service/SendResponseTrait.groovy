package org.jetbrains.plugins.verifier.service

import com.google.gson.Gson

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
      json = new Gson().toJson(obj)
    }
    render(contentType: 'text/json', encoding: 'utf-8', text: json)
  }


}

