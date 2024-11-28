/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.options

import com.jetbrains.pluginverifier.output.OutputFormat
import com.sampullara.cli.Argument

open class CmdOpts(
  @set:Argument("verification-reports-dir", alias = "vrd", description = "The directory where the verification report files will reside")
  var verificationReportsDir: String? = null,

  @set:Argument("verification-reports-formats", alias="vrf", description = "Output format of the verification report files. Supported formats are 'plain' (console output in stdout), 'html' and 'markdown'")
  var outputFormats: Array<String> = arrayOf(OutputFormat.PLAIN.code(), OutputFormat.HTML.code()),

  @set:Argument("ignored-problems", alias = "ip", description = "The problems specified in this file will be ignored. The file must contain lines in form <plugin_xml_id>:<plugin_version>:<problem_description_regexp_pattern>")
  var ignoreProblemsFile: String? = null,

  @set:Argument("runtime-dir", alias = "r", description = "The path to directory containing Java runtime jars (e.g. /usr/lib/jvm/java-8-oracle). If not specified, the JDK from 'JAVA_HOME' will be chosen.")
  var runtimeDir: String? = null,

  @set:Argument("team-city", alias = "tc", description = "Specify this flag if you want to print the TeamCity compatible output on stdout.")
  var needTeamCityLog: Boolean = false,

  @set:Argument("offline", alias = "offline", description = "Specify this flag if the Plugin Verifier must use only locally downloaded dependencies of plugins")
  var offlineMode: Boolean = false,

  @set:Argument("tc-grouping", alias = "g", description = "How to group the TeamCity presentation of the problems: either 'plugin' to group by each plugin or 'problem_type' to group by problem type")
  var teamCityGroupType: String? = null,

  @set:Argument("previous-tc-tests-file", description = "File containing TeamCity tests that were run in the previous build. ")
  var previousTcTestsFile: String? = null,

  @set:Argument("plugins-to-check-all-builds", alias = "p-all", delimiter = ":", description = "The plugin IDs to check with IDE. The plugin verifier will check ALL compatible plugin builds")
  var pluginToCheckAllBuilds: Array<String> = arrayOf(),

  @set:Argument("plugins-to-check-last-builds", alias = "p-last", delimiter = ":", description = "The plugin IDs to check with IDE. The plugin verifier will check LAST plugin build only")
  var pluginToCheckLastBuild: Array<String> = arrayOf(),

  @set:Argument(
    "plugins-to-check-file", alias = "ptcf", description = "File that contains list of plugins to check" +
    "Each line of the file is either '<plugin_id>' (check ALL builds of the plugin) or '@<plugin_id>' (check only LAST build of the plugin)."
  )
  var pluginsToCheckFile: String? = null,

  @set:Argument("external-prefixes", alias = "ex-prefixes", delimiter = ":", description = "The prefixes of classes from the external libraries. The Verifier will not report 'No such class' for such classes.")
  var externalClassesPrefixes: Array<String> = arrayOf(),

  @set:Argument(
    "exclude-external-build-classes-selector", alias = "ex-selector", description = "Specify this flag if the Plugin Verifier must exclude selector for classes " +
    "used for the external build processes such as JPS classes bundled into the Kotlin plugin (`/lib/jps`).")
  var excludeExternalBuildClassesSelector: Boolean = false,

  @set:Argument(
    "subsystems-to-check",
    alias = "subsystems",
    description = "Specifies which subsystems of IDE should be checked. Available options: all (default), android-only, without-android.\n" +
      "\tall - verify all code\n" +
      "\tandroid-only - verify only code related to Android support.\n" +
      "\twithout-android - exclude problems related to Android support. "
  )
  var subsystemsToCheck: String = "all",

  @set:Argument(
    "keep-only-problems",
    alias = "kop",
    description = "Only the problems matching lines in this file will be reflected in report. The file must contain lines in form: <problem_description_regexp_pattern>"
  )
  var keepOnlyProblemsFile: String? = null,

  @set:Argument(
    "suppress-internal-api-usages",
    description = "Suppress internal API usage checks. Available options: no (default), jetbrains-plugins."
  )
  var suppressInternalApiUsageWarnings: String = "no",

  @set:Argument(
  "submission-type",
  description = "Set the plugin submission type for verifications. Available options: new (a first-time submission for new plugins with stricter set of verification rules), existing (a plugin that has been already submitted and validated with a relaxed set of rules)."
  )
  var submissionType: String? = "new",

  @set:Argument(
  "mute",
    alias = "muted-problems",
    description = "The problems that will be ignored. Comma-separated list of plugin problem identifiers.\n" +
      "\tExample: -mute ForbiddenPluginIdPrefix,TemplateWordInPluginId"
  )
  var mutedPluginProblems: Array<String> = emptyArray(),

  @set:Argument(
    "missing-layout-classpath-file",
    description = "Sets the behavior when a product info layout declares a missing classpath. " +
      "Available options: skip-warn (skip the entire layout component and log a warning, the default); " +
      "skip-silently (skip the entire layout component);  " +
      "fail (fails the verification with an error indicating an incorrect IDE); " +
      "ignore (process the layout component as is)"
    )
  var missingLayoutClasspathFile: String? = "skip-warn"
)