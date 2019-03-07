package com.jetbrains.pluginverifier.network

abstract class BaseNetworkException : RuntimeException {

  val serverUrl: String

  constructor(serverUrl: String) {
    this.serverUrl = serverUrl
  }

  constructor(serverUrl: String, cause: Throwable) : super(cause) {
    this.serverUrl = serverUrl
  }
}

class NotFound404ResponseException(serverUrl: String) : BaseNetworkException(serverUrl) {
  override val message
    get() = "Server $serverUrl cannot find the resource: HTTP Response 404"
}

class ServerInternalError500Exception(serverUrl: String) : BaseNetworkException(serverUrl) {
  override val message
    get() = "Server $serverUrl has faced unexpected problems: HTTP Response 500"
}

class ServerUnavailable503Exception(serverUrl: String) : BaseNetworkException(serverUrl) {
  override val message
    get() = "Server $serverUrl is currently unavailable: HTTP Response 503"
}

class NonSuccessfulResponseException(serverUrl: String, val responseCode: Int, val errorMessage: String) : BaseNetworkException(serverUrl) {
  override val message
    get() = "Server $serverUrl HTTP Response $responseCode: $errorMessage"
}

class FailedRequestException(serverUrl: String, cause: Throwable) : BaseNetworkException(serverUrl, cause) {
  override val message
    get() = "Unable to communicate with $serverUrl" + (cause?.message?.let { ": $it" } ?: "")
}