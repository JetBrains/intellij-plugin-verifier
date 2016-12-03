package org.jetbrains.plugins.verifier.service

import com.google.common.collect.ImmutableList

class StatusController implements SendResponseTrait {

  private static final def SUPPORTED_CLIENTS = ImmutableList.of("1.0")

  def index() {}

  def supportedClients() {
    sendJson(SUPPORTED_CLIENTS)
  }

}
