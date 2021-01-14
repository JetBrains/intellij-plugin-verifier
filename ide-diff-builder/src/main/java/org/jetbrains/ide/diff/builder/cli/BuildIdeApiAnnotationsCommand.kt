/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.repositories.IntelliJIdeRepository
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.ide.diff.builder.api.*
import org.jetbrains.ide.diff.builder.filter.AndClassFilter
import org.jetbrains.ide.diff.builder.filter.ClassFilter
import org.jetbrains.ide.diff.builder.filter.NonImplementationClassFilter
import org.jetbrains.ide.diff.builder.persistence.externalAnnotations.ExternalAnnotationsApiReportWriter
import org.jetbrains.ide.diff.builder.persistence.json.JsonApiReportWriter
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Builds API annotations artifacts for IDEs from the IntelliJ Artifacts Repositories
 * and saves them under results directory with names like `ideaIU-191.1234-annotations.zip`.
 */
class BuildIdeApiAnnotationsCommand : Command {

  companion object {
    private val LOG = LoggerFactory.getLogger("build-api-annotations")

    val MIN_BUILD_NUMBER = IdeVersion.createIdeVersion("145.1")
  }

  override val commandName: String
    get() = "build-api-annotations"

  override val help: String
    get() = """
      Builds API annotations artifacts for IDEs that lack such annotations in the IntelliJ Artifacts Repositories:
      https://www.jetbrains.com/intellij-repository/releases/ and https://www.jetbrains.com/intellij-repository/snapshots
      It saves them under results directory with names like `ideaIU-191.1234-annotations.zip`.

      build-api-annotations [-ides-dir <IDE cache dir] [-jdk-path <path to JDK home>] [-packages "org.some;com.another"] <results directory>
    """.trimIndent()

  open class CliOptions : IdeDiffCommand.CliOptions() {
    @set:Argument("ides-dir", description = "Path where downloaded IDE builds are cached")
    var idesDirPath: String? = null

    fun getIdesDirectory(): Path =
      if (idesDirPath != null) {
        Paths.get(idesDirPath!!)
      } else {
        Files.createTempDirectory("ides-dir").also {
          it.toFile().deleteOnExit()
        }
      }
  }

  override fun execute(freeArgs: List<String>) {
    val cliOptions = CliOptions()
    val args = Args.parse(cliOptions, freeArgs.toTypedArray(), false)

    val resultsDirectory = Paths.get(args[0])
    resultsDirectory.createDir()
    LOG.info("Results will be saved to $resultsDirectory")

    val jdkPath = cliOptions.getJdkPath()
    LOG.info("JDK will be used to resolve java classes: $jdkPath")

    val classFilter = cliOptions.classFilter()
    LOG.info(classFilter.toString())

    val idesDir = cliOptions.getIdesDirectory()
    LOG.info("IDE cache directory to use: $idesDir")

    val ideFilesBank = createIdeFilesBank(idesDir)

    val repositoryToIdes = allIdeRepositories.associateWith { repository ->
      repository
        .fetchIndex()
        .filter { it.version.productCode == "IU" && it.version >= MIN_BUILD_NUMBER }
        .sortedBy { it.version }
    }

    val allIdesToProcess = repositoryToIdes.flatMap { it.value }.distinctBy { it.version }.sortedBy { it.version }

    LOG.info("The following ${allIdesToProcess.size} IU IDEs (> $MIN_BUILD_NUMBER) are available in all IDE repositories: " + allIdesToProcess.joinToString())

    val metadata = BuildIdeApiMetadata().buildMetadata(
      allIdesToProcess,
      ideFilesBank,
      jdkPath,
      classFilter,
      resultsDirectory
    )

    val metadataPath = resultsDirectory.resolve("metadata.json")
    JsonApiReportWriter().saveReport(metadata, metadataPath)
    LOG.info("The API metadata has been saved to ${metadataPath.simpleName}.")

    LOG.info("Building annotations for last IDEs of each branch.")
    val lastBranchIdes = repositoryToIdes.values
      .flatMap { ides ->
        ides
          .groupBy { it.version.baselineVersion }
          .mapValues { (_, branchIdes) -> branchIdes.maxBy { it.version }!! }
          .values
      }
      .map { it.version }
      .distinct()
      .sorted()

    LOG.info("Last branch IDEs: $lastBranchIdes")

    val annotationsClassFilter = AndClassFilter(listOf(classFilter, NonImplementationClassFilter))
    buildExternalAnnotations(metadata, resultsDirectory, lastBranchIdes, annotationsClassFilter)
  }

  private fun buildExternalAnnotations(
    metadata: ApiReport,
    resultsDirectory: Path,
    ides: List<IdeVersion>,
    classFilter: ClassFilter
  ) {
    for (ideVersion in ides) {
      LOG.info("Building annotations for $ideVersion")
      val artifactId = IntelliJIdeRepository.getArtifactIdByProductCode(ideVersion.productCode)
      checkNotNull(artifactId) { ideVersion.asString() }
      val resultPath = resultsDirectory.resolve("$artifactId-${ideVersion.asStringWithoutProductCode()}-annotations.zip")
      val annotations = buildApiAnnotations(metadata, ideVersion, classFilter)
      ExternalAnnotationsApiReportWriter().saveReport(annotations, resultPath)
    }
  }

  private fun buildApiAnnotations(metadata: ApiReport, ideVersion: IdeVersion, classFilter: ClassFilter): ApiReport {
    val apiSignatureToEvents = hashMapOf<ApiSignature, Set<ApiEvent>>()

    for ((signature, allEvents) in metadata.apiSignatureToEvents) {
      val className = when (signature) {
        is ClassSignature -> signature.className
        is MethodSignature -> signature.hostSignature.className
        is FieldSignature -> signature.hostSignature.className
      }
      if (!classFilter.shouldProcessClass(className)) {
        continue
      }

      val events = allEvents.filter { it is IntroducedIn || it is RemovedIn }.sortedBy { it.ideVersion }
      if (events.isEmpty()) {
        continue
      }
      val sanitizedEvents = arrayListOf<ApiEvent>()

      val firstEvent = events.first()
      if (firstEvent is IntroducedIn) {
        if (firstEvent.ideVersion.baselineVersion > ideVersion.baselineVersion) {
          //Too new signature for this IDE => skip
          continue
        }
        sanitizedEvents += firstEvent
      }

      val lastEvent = events.last()
      if (lastEvent is RemovedIn) {
        if (lastEvent.ideVersion.baselineVersion < ideVersion.baselineVersion) {
          //Too old signature for this IDE => skip
          continue
        }
        sanitizedEvents += lastEvent
      }

      apiSignatureToEvents[signature] = sanitizedEvents.toSet()
    }
    return ApiReport(ideVersion, apiSignatureToEvents)
  }

  private fun createIdeFilesBank(idesDir: Path): IdeFilesBank {
    val gigabytes = System.getProperty("ides.dir.max.size.gb", "10").toInt()
    val diskSpaceSetting = DiskSpaceSetting(SpaceAmount.ONE_GIGO_BYTE * gigabytes)
    return IdeFilesBank(idesDir, allIdeRepository, diskSpaceSetting)
  }


}