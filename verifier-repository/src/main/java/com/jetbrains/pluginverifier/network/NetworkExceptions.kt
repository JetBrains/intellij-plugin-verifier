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
  override val message = "Server $serverUrl cannot find the resource: HTTP Response 404"
}

class ServerInternalError500Exception(serverUrl: String) : BaseNetworkException(serverUrl) {
  override val message = "Server $serverUrl has faced unexpected problems: HTTP Response 500"
}

class ServerUnavailable503Exception(serverUrl: String) : BaseNetworkException(serverUrl) {
  override val message = "Server $serverUrl is currently unavailable: HTTP Response 503"
}

class NonSuccessfulResponseException(serverUrl: String, responseCode: Int, errorMessage: String) : BaseNetworkException(serverUrl) {
  override val message = "Server $serverUrl HTTP Response $responseCode: $errorMessage"
}

class FailedRequestException(serverUrl: String, reason: Throwable) : BaseNetworkException(serverUrl, reason) {
  override val message = "Unable to communicate with $serverUrl: ${reason.message}"
}