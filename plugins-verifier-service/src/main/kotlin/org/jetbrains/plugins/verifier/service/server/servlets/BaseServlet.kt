package org.jetbrains.plugins.verifier.service.server.servlets

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.typeToken
import com.google.gson.Gson
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.startup.ServerStartupListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.jar.Manifest
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * The base servlet that contains a reference to the [context] [serverContext]
 * and utility methods used for communication.
 */
abstract class BaseServlet : HttpServlet() {

  companion object {
    protected val LOG: Logger = LoggerFactory.getLogger(BaseServlet::class.java)

    val GSON: Gson = Gson()

    @JvmStatic
    protected fun getPath(req: HttpServletRequest, resp: HttpServletResponse): String? {
      val path = req.pathInfo
      if (path == null) {
        sendNotFound(resp)
        return null
      }
      return path
    }

    @JvmStatic
    protected fun sendNotFound(resp: HttpServletResponse, message: String = "") {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, message)
    }

    inline fun <reified T : Any> fromJson(inputStream: InputStream): T = GSON.fromJson(InputStreamReader(inputStream, StandardCharsets.UTF_8), typeToken<T>())

    @JvmStatic
    protected inline fun <reified T : Any> parseJsonPart(req: HttpServletRequest, partName: String): T? {
      val part = req.getPart(partName) ?: return null
      return try {
        fromJson(part.inputStream)
      } catch (e: Exception) {
        LOG.error("Unable to deserialize part $partName", e)
        null
      }
    }

    @JvmStatic
    protected inline fun <reified T : Any> parseJsonParameter(req: HttpServletRequest, parameterName: String): T? {
      val parameter = req.getParameter(parameterName) ?: return null
      return try {
        GSON.fromJson<T>(parameter)
      } catch (e: Exception) {
        LOG.error("Unable to deserialize parameter $parameterName: $parameter", e)
        null
      }
    }

  }

  final override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) = doPost(req, resp)

  protected fun sendContent(resp: HttpServletResponse, bytes: ByteArray, contentType: String) {
    resp.setContentLength(bytes.size)
    resp.contentType = contentType
    resp.outputStream.write(bytes)
    resp.status = HttpServletResponse.SC_OK
  }

  protected fun sendBytes(resp: HttpServletResponse, bytes: ByteArray) {
    sendContent(resp, bytes, "application/octet-stream")
  }

  protected fun sendOk(resp: HttpServletResponse, message: String) {
    sendContent(resp, message.toByteArray(), "text/plain")
  }

  protected fun sendJson(resp: HttpServletResponse, src: Any) {
    resp.contentType = "application/json"
    resp.characterEncoding = "UTF-8"
    val writer = resp.writer
    GSON.toJson(src, writer)
    writer.flush()
  }

  val appVersion: String? by lazy {
    servletContext.getResourceAsStream("/META-INF/MANIFEST.MF")?.use { inputStream ->
      val manifest = Manifest(inputStream)
      manifest.mainAttributes.getValue("Plugin-Verifier-Service-Version")
    }
  }

  protected val serverContext by lazy {
    servletContext.getAttribute(ServerStartupListener.SERVER_CONTEXT_KEY) as ServerContext
  }

}
