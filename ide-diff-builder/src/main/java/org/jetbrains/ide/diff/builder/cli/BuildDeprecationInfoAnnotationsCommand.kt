package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.extension
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import com.sampullara.cli.Args
import org.jetbrains.ide.diff.builder.api.*
import org.jetbrains.ide.diff.builder.filter.ClassFilter
import org.jetbrains.ide.diff.builder.persistence.externalAnnotations.ExternalAnnotationsApiReportWriter
import org.jetbrains.ide.diff.builder.persistence.json.JsonApiReportReader
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * [help]
 */
class BuildDeprecationInfoAnnotationsCommand : Command {

  companion object {
    private val LOG = LoggerFactory.getLogger("build-deprecation-info-annotations")
  }

  override val commandName
    get() = "build-deprecation-info-annotations"

  override val help
    get() = """
      Build IDE external annotations containing information on when IntelliJ APIs were marked deprecated.
      It may be useful to know for how long an API element is marked deprecated in the code.
      
      build-deprecation-info-annotations <metadata.json> <output .zip>
    """.trimIndent()

  override fun execute(freeArgs: List<String>) {
    val cliOptions = CliOptions()
    val args = Args.parse(cliOptions, freeArgs.toTypedArray(), false)
    if (args.size < 2) {
      System.err.println("Paths to <metadata.json> and <output .zip> must be specified")
      exitProcess(1)
    }

    val metadataPath = Paths.get(args[0])
    require(metadataPath.exists()) { "Metadata file does not exist: $metadataPath" }
    require(metadataPath.extension == "json") { "Metadata is not a .json file: $metadataPath" }

    val outputZipArchive = Paths.get(args[1])
    if (outputZipArchive.exists()) {
      outputZipArchive.deleteLogged()
    }

    val classFilter = cliOptions.classFilter()
    LOG.info("External annotations with deprecation info on IntelliJ APIs will be saved to $outputZipArchive")
    LOG.info("Only the following classes will be processed: $classFilter")

    val metadata = JsonApiReportReader().readApiReport(metadataPath)
    val deprecationReport = getReportWithOnlyDeprecationInfo(metadata, classFilter)
    ExternalAnnotationsApiReportWriter().saveReport(deprecationReport, outputZipArchive)
    LOG.info("Annotations have been saved to $outputZipArchive (${outputZipArchive.fileSize})")
  }

  private fun getReportWithOnlyDeprecationInfo(metadata: ApiReport, classFilter: ClassFilter): ApiReport {
    val apiSignatureToEvents = hashMapOf<ApiSignature, Set<ApiEvent>>()
    for ((signature, events) in metadata.apiSignatureToEvents) {
      val className = when (signature) {
        is ClassSignature -> signature.className
        is MethodSignature -> signature.hostSignature.className
        is FieldSignature -> signature.hostSignature.className
      }
      if (!classFilter.shouldProcessClass(className)) {
        continue
      }

      val removedIn = events.filterIsInstance<RemovedIn>().maxBy { it.ideVersion }
      val introducedIn = events.filterIsInstance<IntroducedIn>().maxBy { it.ideVersion }
      if (removedIn != null && (introducedIn == null || introducedIn.ideVersion <= removedIn.ideVersion)) {
        // Skip already removed APIs.
        continue
      }

      val deprecatedIn = events.filterIsInstance<MarkedDeprecatedIn>().maxBy { it.ideVersion }
      if (deprecatedIn != null) {
        apiSignatureToEvents[signature] = setOf(deprecatedIn)
      }
    }
    return ApiReport(metadata.ideBuildNumber, apiSignatureToEvents)
  }

  class CliOptions : IdeDiffCommand.CliOptions()

}