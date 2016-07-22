import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.status.OnConsoleStatusListener
import grails.util.BuildSettings
import grails.util.Environment

statusListener OnConsoleStatusListener

//http://logback.qos.ch/manual/groovy.html
appender('STDOUT', ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = "%d{yyyy-MM-dd'T'HH:mm:ss,SSS} [%thread] %-5level %logger{36} - %msg%n"
  }
}

appender('FILE', FileAppender) {
  def home = System.getProperty('verifierService.homeDirectory')
  def logName = "log"
  file = "$home/logs/$logName"
  append = true
  encoder(PatternLayoutEncoder) {
    pattern = "%d{yyyy-MM-dd'T'HH:mm:ss,SSS} [%thread] %-5level %logger{36} - %msg%n"
  }
}

logger("com.jetbrains.pluginverifier.misc.DownloadManager", OFF)
logger("com.intellij.structure", ERROR, ['STDOUT'])
logger("com.jetbrains.pluginverifier", INFO, ['FILE'])
logger("grails.app.controllers", DEBUG, ['STDOUT'])
logger("grails.app.services", DEBUG, ['STDOUT'])

root(ERROR, ['STDOUT'])

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode() && targetDir) {
  appender("FULL_STACKTRACE", FileAppender) {
    file = "${targetDir}/stacktrace.log"
    append = true
    encoder(PatternLayoutEncoder) {
      pattern = "%level %logger - %msg%n"
    }
  }
  logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
}
