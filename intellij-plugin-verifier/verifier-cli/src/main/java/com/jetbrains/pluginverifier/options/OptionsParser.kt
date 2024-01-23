/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.pluginverifier.filtering.*
import com.jetbrains.pluginverifier.filtering.documented.DocumentedProblemsFilter
import com.jetbrains.pluginverifier.filtering.documented.DocumentedProblemsPagesFetcher
import com.jetbrains.pluginverifier.filtering.documented.DocumentedProblemsParser
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeDownloader
import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import com.jetbrains.pluginverifier.ide.repositories.IntelliJIdeRepository
import com.jetbrains.pluginverifier.ide.repositories.ReleaseIdeRepository
import com.jetbrains.pluginverifier.output.*
import com.jetbrains.pluginverifier.output.teamcity.TeamCityHistory
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.verifiers.packages.DefaultPackageFilter
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object OptionsParser {

  private val LOG = LoggerFactory.getLogger(OptionsParser::class.java)

  private val TIMESTAMP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH.mm.ss")

  private fun getVerificationReportsDirectory(opts: CmdOpts): Path {
    val reportDirectory = Paths.get(opts.verificationReportsDir ?: newVerificationDirectoryName())
    if (reportDirectory.exists() && reportDirectory.listFiles().isNotEmpty()) {
      LOG.info("The verification directory ${reportDirectory.toAbsolutePath()} is being deleted because it is not empty.")
      reportDirectory.deleteLogged()
    }
    reportDirectory.createDir()
    return reportDirectory
  }

  private fun newVerificationDirectoryName(): String {
    val nowTime = TIMESTAMP_DATE_FORMAT.format(LocalDateTime.now())
    return "verification-$nowTime".replaceInvalidFileNameCharacters()
  }

  fun parseOutputOptions(opts: CmdOpts): OutputOptions {
    val verificationReportsDirectory = getVerificationReportsDirectory(opts)
    println(VERIFICATION_REPORTS_DIRECTORY.format(verificationReportsDirectory))
    val teamCityLog = if (opts.needTeamCityLog) TeamCityLog(System.out) else null
    val previousTcHistory = opts.previousTcTestsFile?.let { Paths.get(it) }?.let { TeamCityHistory.readFromFile(it) }
    val outputFormats = parseOutputFormats(opts)
    return OutputOptions(
      verificationReportsDirectory,
      teamCityLog,
      TeamCityResultPrinter.GroupBy.parse(opts.teamCityGroupType),
      previousTcHistory,
      outputFormats
    )
  }

  private fun parseOutputFormats(opts: CmdOpts): List<OutputFormat> {
    if (opts.outputFormats.isExplicitlyEmpty()) {
      return emptyList()
    }
    val (exclusions, inclusions) = opts.outputFormats.partition { it.trim().startsWith("-") }

    val includedFormats = inclusions.mapNotNull {
      try {
        OutputFormat.valueOf(it.uppercase())
      } catch (e: IllegalArgumentException) {
        LOG.warn("Unsupported verification report output format '$it'. Skipping this output format.")
        null
      }
    }.ifEmpty { DEFAULT_OUTPUT_FORMATS }

    val excludedFormats = exclusions.map { it.removePrefix("-") }
      .mapNotNull {
        try {
          OutputFormat.valueOf(it.uppercase())
        } catch (e: IllegalArgumentException) {
          LOG.warn("Unsupported verification report output format '$it' specified for exclusion. Ignoring this output format.")
          null
        }
      }.toSet()
     return (includedFormats - excludedFormats).ifEmpty {
       LOG.warn("No supported output formats were set. Falling back to the default set")
       DEFAULT_OUTPUT_FORMATS
     }
  }

  fun createIdeDescriptor(ide: String, opts: CmdOpts): IdeDescriptor {
    val ideFile = if (ide.startsWith("[") && ide.endsWith("]")) {
      downloadIde(ide)
    } else {
      Paths.get(ide)
    }
    require(ideFile.isDirectory) { "IDE must reside in a directory: $ideFile" }
    LOG.info(READING_IDE_FROM.format(ideFile))
    return createIdeDescriptor(ideFile, opts)
  }

  fun createIdeDescriptor(idePath: Path, opts: CmdOpts): IdeDescriptor {
    val defaultJdkPath = opts.runtimeDir?.let {
      Paths.get(it).also {
        LOG.info("Using Java runtime from $it")
      }
    }
    return IdeDescriptor.create(idePath, defaultJdkPath, null)
  }

  fun createPluginParsingConfiguration(opts: CmdOpts): PluginParsingConfiguration {
     val submissionType = when (opts.submissionType) {
      "existing" -> SubmissionType.EXISTING
      "new" -> SubmissionType.NEW
      else -> SubmissionType.NEW
    }
    return PluginParsingConfiguration(submissionType)
  }

  private val ideLatestRegexp = Regex("\\[latest(-([A-Z]+))?]")
  private val ideLatestReleaseRegexp = Regex("\\[latest-release(-([A-Z]+))?]")

  private fun downloadIde(ide: String): Path {
    val latestMatch = ideLatestRegexp.matchEntire(ide)
    if (latestMatch != null) {
      val productCode = latestMatch.groups[2]?.value ?: "IU"
      val repository = if (productCode == "IU") {
        IntelliJIdeRepository(IntelliJIdeRepository.Channel.SNAPSHOTS)
      } else {
        ReleaseIdeRepository()
      }
      return downloadIde(productCode, repository, true)
    }

    val latestReleaseMatch = ideLatestReleaseRegexp.matchEntire(ide)
    if (latestReleaseMatch != null) {
      val productCode = latestReleaseMatch.groups[2]?.value ?: "IU"
      return downloadIde(productCode, ReleaseIdeRepository(), false)
    }
    throw IllegalArgumentException("IDE pattern does not match any of: ${ideLatestRegexp.pattern}, ${ideLatestReleaseRegexp.pattern}")
  }

  private fun downloadIde(
    productCode: String,
    ideRepository: IdeRepository,
    latestOrLatestRelease: Boolean
  ): Path {
    val releaseModifier = (if (latestOrLatestRelease) "latest" else "latest release") + " of $productCode"
    LOG.info("Requesting the index of available IDE builds for the $releaseModifier from $ideRepository")
    val availableIde = ideRepository.fetchIndex()
      .filter { it.product.productCode == productCode }
      .filter { if (latestOrLatestRelease) true else it.isRelease }
      .maxByOrNull { it.version }
    availableIde ?: throw IllegalArgumentException("No IDE found for $productCode in $ideRepository")

    val idesDirectory = System.getProperty("intellij.plugin.verifier.download.ide.temp.dir")?.let { Paths.get(it) }
      ?: Path.of(System.getProperty("java.io.tmpdir")).resolve("downloaded-ides")

    val ideDirectory = idesDirectory.resolve(availableIde.version.asString())
    if (Files.isDirectory(ideDirectory)) {
      LOG.info("IDE $releaseModifier is already downloaded to $ideDirectory")
      return ideDirectory
    }

    val downloadingTempDir = idesDirectory.resolve("downloading").createDir()
    LOG.info("Downloading $releaseModifier ${availableIde.version} from $ideRepository to $ideDirectory")
    return when (val downloadResult = IdeDownloader().download(availableIde, downloadingTempDir)) {
      is DownloadResult.Downloaded -> {
        Files.move(downloadResult.downloadedFileOrDirectory, ideDirectory)
        ideDirectory
      }
      is DownloadResult.NotFound -> throw IllegalArgumentException("No IDE found for $productCode in $ideRepository")
      is DownloadResult.FailedToDownload -> throw RuntimeException("Failed to download IDE $productCode", downloadResult.error)
    }
  }

  fun getExternalClassesPackageFilter(opts: CmdOpts): PackageFilter =
    opts.externalClassesPrefixes
      .map { it.replace('.', '/') }
      .let { list ->
        DefaultPackageFilter(list.map { DefaultPackageFilter.Descriptor(true, it) })
      }

  private fun createIgnoredProblemsFilter(opts: CmdOpts): ProblemsFilter? {
    if (opts.ignoreProblemsFile != null) {
      val file = Paths.get(opts.ignoreProblemsFile!!)
      require(file.exists()) { "Ignored problems file doesn't exist $file" }
      return getIgnoreFilter(file)
    }
    return null
  }

  private fun createKeepOnlyProblemsFilter(opts: CmdOpts): ProblemsFilter? {
    if (opts.keepOnlyProblemsFile != null) {
      val file = Paths.get(opts.keepOnlyProblemsFile!!)
      require(file.exists()) { "Keep only problems file doesn't exist $file" }
      return getKeepOnlyFilter(file)
    }
    return null
  }

  /**
   * Determines which subsystem should be verified in this task.
   *
   * Whether we would like to track only IDEA related problems (-without-android),
   * or only Android related problems (MP-1377) (-android-only),
   * or both IDEA and Android problems (-all).
   */
  private fun createSubsystemProblemsFilter(opts: CmdOpts) =
    when (opts.subsystemsToCheck) {
      "android-only" -> AndroidProblemsFilter()
      "without-android" -> IdeaOnlyProblemsFilter()
      else -> null
    }

  fun getProblemsFilters(opts: CmdOpts): List<ProblemsFilter> {
    val keepOnlyProblemsFilter = createKeepOnlyProblemsFilter(opts)
    val ignoredProblemsFilter = createIgnoredProblemsFilter(opts)
    val documentedProblemsFilter = try {
      if (opts.offlineMode) null else createDocumentedProblemsFilter()
    } catch (e: Exception) {
      LOG.error("Unable to read documented IntelliJ API incompatible changes. Corresponding API problems won't be ignored.", e)
      null
    }
    val codeProblemsFilter = createSubsystemProblemsFilter(opts)
    return listOfNotNull(keepOnlyProblemsFilter, ignoredProblemsFilter, documentedProblemsFilter, codeProblemsFilter)
  }

  private fun createDocumentedProblemsFilter(): DocumentedProblemsFilter {
    val documentedPages = DocumentedProblemsPagesFetcher().fetchPages()
    val documentedProblemsParser = DocumentedProblemsParser(true)
    val documentedProblems = documentedPages.flatMap { documentedProblemsParser.parse(it.pageBody) }
    return DocumentedProblemsFilter(documentedProblems)
  }

  private fun getIgnoreFilter(ignoreProblemsFile: Path): IgnoredProblemsFilter {
    val ignoreConditions = arrayListOf<IgnoreCondition>()
    try {
      ignoreProblemsFile.forEachLine { lineT ->
        val line = lineT.trim()
        if (line.isBlank() || line.startsWith("//")) {
          //it is a comment
          return@forEachLine
        }
        ignoreConditions.add(IgnoreCondition.parseCondition(line))
      }
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      throw IllegalArgumentException("Unable to parse ignored problems file $ignoreProblemsFile", e)
    }

    return IgnoredProblemsFilter(ignoreConditions)
  }

  private fun getKeepOnlyFilter(keepOnlyProblemsFile: Path): KeepOnlyProblemsFilter {
    val keepOnlyConditions = arrayListOf<KeepOnlyCondition>()
    try {
      keepOnlyProblemsFile.forEachLine { lineT ->
        val line = lineT.trim()
        if (line.isBlank() || line.startsWith("//")) {
          //it is a comment
          return@forEachLine
        }
        keepOnlyConditions.add(KeepOnlyCondition.parseCondition(line))
      }
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      throw IllegalArgumentException("Unable to parse keep only problems file $keepOnlyProblemsFile", e)
    }

    return KeepOnlyProblemsFilter(keepOnlyConditions)
  }

  /**
   * Indicates that array has a single element -- an empty string.
   * This is to handle array-like arguments passed from CLI.
   */
  private fun Array<String>.isExplicitlyEmpty(): Boolean = size == 1 && first().isEmpty()

}

