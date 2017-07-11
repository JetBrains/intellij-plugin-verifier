package org.jetbrains.plugins.verifier.service.servlets

import com.jetbrains.pluginverifier.misc.bytesToGigabytes
import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import com.jetbrains.pluginverifier.output.HtmlBuilder
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.service.ServerInstance
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.status.ServerStatus
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class InfoServlet : BaseServlet() {

  private val serverStatus = ServerStatus(getTaskManager())

  companion object {
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
  }

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    processStatus(resp)
  }

  private fun processStatus(resp: HttpServletResponse) {
    sendBytes(resp, generateStatusPage(), "text/html")
  }

  private fun generateStatusPage(): ByteArray {
    val byteOS = ByteArrayOutputStream()
    val printWriter = PrintWriter(byteOS)
    HtmlBuilder(printWriter).apply {
      html {
        head {
          title("Server status")
          style {
            +"""table, th, td {
              border: 1px solid black;
              border-collapse: collapse;
            }"""
          }
        }
        body {
          div {
            h2 {
              +"Application parameters:"
            }
            ul {
              Settings.values().forEach { s ->
                li {
                  +(s.key + " = " + if (s.encrypted) "*****" else s.get())
                }
              }
            }

            h2 {
              +"Status:"
            }
            ul {
              val (totalMemory, freeMemory, usedMemory, maxMemory) = serverStatus.getMemoryInfo()
              li { +"Total memory: ${totalMemory.bytesToMegabytes()} Mb" }
              li { +"Free memory: ${freeMemory.bytesToMegabytes()} Mb" }
              li { +"Used memory: ${usedMemory.bytesToMegabytes()} Mb" }
              li { +"Max memory: ${maxMemory.bytesToMegabytes()} Mb" }

              val (totalUsage) = serverStatus.getDiskUsage()
              li { +"Total disk usage: ${totalUsage.bytesToGigabytes()} Gb" }
            }

            h2 {
              +"Available IDEs: "
            }
            ul {
              IdeFilesManager.ideList().forEach {
                li {
                  +it.toString()
                }
              }
            }

            h2 {
              +("Updates missing compatible IDEs: ")
            }
            +ServerInstance.verifierService.updatesMissingCompatibleIde.joinToString()

            h2 {
              +"Running tasks"
            }
            table("width: 100%") {
              tr {
                th { +"ID" }
                th { +"Task name" }
                th { +"Start time" }
                th { +"Status" }
                th { +"Completion %" }
                th { +"Total time (ms)" }
              }

              serverStatus.getRunningTasks().forEach { (taskId, taskName, startedDate, state, progress, totalTimeMs) ->
                tr {
                  td { +taskId.toString() }
                  td { +taskName }
                  td { +DATE_FORMAT.format(startedDate) }
                  td { +state.toString() }
                  td { +(progress * 100.0).toString() }
                  td { +(totalTimeMs.toString()) }
                }
              }
            }
          }
        }
      }
    }
    printWriter.close()
    return byteOS.toByteArray()
  }

}
