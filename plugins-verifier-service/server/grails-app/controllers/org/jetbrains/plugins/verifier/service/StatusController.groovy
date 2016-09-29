package org.jetbrains.plugins.verifier.service

import com.google.common.collect.ImmutableList
import com.jetbrains.pluginverifier.persistence.GsonHolder

class StatusController {

  private static final def SUPPORTED_CLIENTS = ImmutableList.of("1.0")

  def index() {}

  def supportedClients() {
    sendJson(SUPPORTED_CLIENTS)
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
