import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.status.OnConsoleStatusListener
import grails.util.BuildSettings
import grails.util.Environment

statusListener OnConsoleStatusListener

String timedPattern = "%d{yyyy-MM-dd'T'HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

//http://logback.qos.ch/manual/groovy.html
appender('STDOUT', ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = timedPattern
  }
}

appender('FILE', FileAppender) {
  def home = System.getProperty('verifierService.homeDirectory')
  def logName = "log"
  file = "$home/logs/$logName"
  append = true
  encoder(PatternLayoutEncoder) {
    pattern = timedPattern
  }
}

logger("com.jetbrains.pluginverifier.misc.DownloadManager", OFF)
logger("com.intellij.structure", ERROR, ['STDOUT'])
logger("com.jetbrains.pluginverifier.api.VManager", TRACE, ['STDOUT'], false)
logger("com.jetbrains.pluginverifier", INFO, ['STDOUT'])
logger("grails.app.controllers", DEBUG, ['STDOUT'], false)
logger("grails.app.services", DEBUG, ['STDOUT'], false)

root(ERROR, ['STDOUT'])

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode() && targetDir) {
  appender("FULL_STACKTRACE", FileAppender) {
    file = "${targetDir}/stacktrace.log"
    append = true
    encoder(PatternLayoutEncoder) {
      pattern = timedPattern
    }
  }
  logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
}
