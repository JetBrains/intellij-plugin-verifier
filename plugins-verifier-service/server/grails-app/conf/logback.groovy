import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.status.OnConsoleStatusListener
import ch.qos.logback.core.util.FileSize

statusListener OnConsoleStatusListener

String timedPattern = "%20(%d{yyyy-MM-dd'T'HH:mm:ss}) %-22([%thread]) %-5level %-30.30(%logger{0}:%method) - %msg%n"

//http://logback.qos.ch/manual/groovy.html
appender('STDOUT', ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = timedPattern
  }
}

def home = System.getProperty('VERIFIER_SERVICE_HOME_DIRECTORY')
if (!home) {
  throw new RuntimeException("The property 'VERIFIER_SERVICE_HOME_DIRECTORY' is not set")
}
appender('FILE', RollingFileAppender) {
  //the logger is always logging into this file (but rollovers the files according to fileNamePattern)
  file = "$home/log/verifier.log"

  rollingPolicy(SizeAndTimeBasedRollingPolicy) {
    fileNamePattern = "$home/log/verifier/%d{MM/dd, aux}/verifier.%d{yyyy-MM-dd_HH}.%i.log"
    //keep logs for 30 days
    maxHistory = 30 * 24
    maxFileSize = FileSize.valueOf("100MB")
    totalSizeCap = FileSize.valueOf("2GB")
  }

  append = true
  encoder(PatternLayoutEncoder) {
    pattern = timedPattern
  }

}

appender('ERRORS_FILE', RollingFileAppender) {
  file = "$home/log/errors.log"
  rollingPolicy(SizeAndTimeBasedRollingPolicy) {
    fileNamePattern = "$home/log/errors/%d{MM/dd, aux}/errors.%d{yyyy-MM-dd}.%i.log"
    maxHistory = 30
    maxFileSize = FileSize.valueOf("100MB")
    totalSizeCap = FileSize.valueOf("2GB")
  }
  append = true
  encoder(PatternLayoutEncoder) {
    pattern = timedPattern
  }
  filter(ThresholdFilter) {
    level = WARN
  }

}

boolean isProduction = 'true' == System.getProperty('verifierService.productionMode')
def loggers = ['STDOUT', 'ERRORS_FILE', 'FILE']

logger("com.jetbrains.pluginverifier.misc.DownloadManager", ERROR, ['STDOUT', 'FILE'], false)
logger("com.jetbrains.pluginverifier.repository.RepositoryManager", ERROR, loggers, false)
logger("com.intellij.structure", ERROR, loggers, false)
logger("org.jetbrains.plugins.verifier.service", isProduction ? INFO : TRACE, loggers, false)
logger("com.jetbrains.pluginverifier.api.VManager", isProduction ? INFO : TRACE, loggers, false)
logger("com.jetbrains.pluginverifier", isProduction ? INFO : TRACE, loggers, false)
logger("grails.app.controllers", isProduction ? INFO : DEBUG, loggers, false)
logger("grails.app.services", isProduction ? INFO : DEBUG, loggers, false)

root(WARN, loggers)