package com.jetbrains.pluginverifier.network

import java.lang.RuntimeException

abstract class BaseNetworkException : RuntimeException()

class NotFound404ResponseException(serverUrl: String) : BaseNetworkException() {
  override val message: String = "HTTP Response 404: Resource is not found ($serverUrl)"
}

class Server500ResponseException(serverUrl: String) : BaseNetworkException() {
  override val message: String = "Server $serverUrl has faced unexpected problems: HTTP Response 500"
}

class NonSuccessfulResponseException(code: Int, serverUrl: String, errorMessage: String) : BaseNetworkException() {
  override val message: String = "Server $serverUrl HTTP Response $code: $errorMessage"
}