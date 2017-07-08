package org.jetbrains.plugins.verifier.service.servlets

import com.jetbrains.pluginverifier.output.HtmlBuilder
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.service.ServerInstance
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.status.ServerStatus
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class InfoServlet(taskManager: TaskManager) : BaseServlet(taskManager) {

  private val serverStatus = ServerStatus(taskManager)

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    processStatus(resp)
  }

  private fun processStatus(resp: HttpServletResponse) {
    sendBytes(resp, generateStatusPage(), "text/html")
  }

  private fun generateStatusPage(): ByteArray {
    val byteOS = ByteArrayOutputStream()
    HtmlBuilder(PrintWriter(byteOS)).apply {
      html {
        head {
          title("Server status")
        }
        body {
          div {
            h2 {
              +"Application parameters:"
              ul {
                Settings.values().forEach { s ->
                  li {
                    +(s.key + " = " + if (s.encrypted) "*****" else s.get())
                  }
                }
              }
            }

            h2 {
              +"Status:"
              ul {
                serverStatus.health().forEach { (key, value) ->
                  +(key + " = " + value)
                }
              }
            }

            h2 {
              +"Available IDEs: "
              ul {
                IdeFilesManager.ideList().forEach {
                  li {
                    +it.toString()
                  }
                }
              }
            }

            h2 {
              +("Updates missing compatible IDEs: " + ServerInstance.verifierService.updatesMissingCompatibleIde.joinToString())
            }

            h2 {
              +"Running tasks"
              ul {
                serverStatus.runningTasks().forEach {
                  li {
                    +it
                  }
                }
              }
            }
          }
        }
      }
    }
    return byteOS.toByteArray()
  }

}
