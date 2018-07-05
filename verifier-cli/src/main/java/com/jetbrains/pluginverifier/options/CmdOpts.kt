package com.jetbrains.pluginverifier.options

import com.sampullara.cli.Argument

open class CmdOpts(
    @set:Argument("verification-reports-dir", alias = "vrd", description = "The directory where the verification report files will reside")
    var verificationReportsDir: String? = null,

    @set:Argument("ignored-problems", alias = "ip", description = "The problems specified in this file will be ignored. The file must contain lines in form <plugin_xml_id>:<plugin_version>:<problem_description_regexp_pattern>")
    var ignoreProblemsFile: String? = null,

    @set:Argument("ide-version", alias = "iv", description = "The actual version of the IDE that will be verified. This value will overwrite the one found in the IDE itself")
    var actualIdeVersion: String? = null,

    @set:Argument("ignore-all-missing-optional-dependencies", alias = "ignore-all-missing-opt-deps", description = "If specified, all the optional missing plugins will not be treated as problems")
    var ignoreAllMissingOptionalDeps: Boolean = false,

    @set:Argument("ignore-specific-missing-optional-dependencies", alias = "ignore-specific-missing-opt-deps", delimiter = ":")
    var ignoreMissingOptionalDeps: Array<String> = arrayOf(),

    @set:Argument("documented-problems-url", alias = "dpu", description = "The URL of the page containing documented problems that must not be reported. " +
        "By default it is $DEFAULT_DOCUMENTED_PROBLEMS_PAGE_URL that contains the sources of the page of " +
        "SDK Documentation: http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html")
    var documentedProblemsPageUrl: String = DEFAULT_DOCUMENTED_PROBLEMS_PAGE_URL,

    @set:Argument("runtime-dir", alias = "r", description = "The path to directory containing Java runtime jars (e.g. /usr/lib/jvm/java-8-oracle). If not specified, the JDK from 'JAVA_HOME' will be chosen.")
    var runtimeDir: String? = null,

    @set:Argument("team-city", alias = "tc", description = "Specify this flag if you want to print the TeamCity compatible output on stdout.")
    var needTeamCityLog: Boolean = false,

    @set:Argument("tc-grouping", alias = "g", description = "How to group the TeamCity presentation of the problems: either 'plugin' to group by each plugin or 'problem_type' to group by problem type")
    var teamCityGroupType: String? = null,

    @set:Argument("plugins-to-check-all-builds", alias = "p-all", delimiter = ":", description = "The plugin IDs to check with IDE. The plugin verifier will check ALL compatible plugin builds")
    var pluginToCheckAllBuilds: Array<String> = arrayOf(),

    @set:Argument("plugins-to-check-last-builds", alias = "p-last", delimiter = ":", description = "The plugin IDs to check with IDE. The plugin verifier will check LAST plugin build only")
    var pluginToCheckLastBuild: Array<String> = arrayOf(),

    @set:Argument("excluded-plugins-file", alias = "epf", description = "File with list of excluded plugin builds (e.g. '<IDE-home>/lib/resources.jar/brokenPlugins.txt'). The verifier will not verify such updates even if they are compatible with IDE.")
    var excludedPluginsFile: String? = null,

    @set:Argument("dump-broken-plugin-list", alias = "d", description = "File to dump broken plugin ids. The broken plugins are those which contain at least one problem as a result of the verification")
    var dumpBrokenPluginsFile: String? = null,

    @set:Argument("plugins-to-check-file", alias = "ptcf", description = "File that contains list of plugins to check (e.g. '<IDE-home>/lib/resources.jar/checkedPlugins.txt'). " +
        "Each line of the file is either '<plugin_id>' (check ALL builds of the plugin) or '@<plugin_id>' (check only LAST build of the plugin).")
    var pluginsToCheckFile: String? = null,

    @set:Argument("external-prefixes", alias = "ex-prefixes", delimiter = ":", description = "The prefixes of classes from the external libraries. The Verifier will not report 'No such class' for such classes.")
    var externalClassesPrefixes: Array<String> = arrayOf(),

    @set:Argument(
        "subsystems-to-check",
        alias = "subsystems",
        description = "Specifies which subsystems of IDE should be checked. Available options: all (default), android-only, without-android.\n" +
            "\tall - verify all code\n" +
            "\tandroid-only - verify only code related to Android support.\n" +
            "\twithout-android - exclude problems related to Android support. "
    )
    var subsystemsToCheck: String = "all"

) {
  companion object {
    const val DEFAULT_DOCUMENTED_PROBLEMS_PAGE_URL = "https://raw.githubusercontent.com/JetBrains/intellij-sdk-docs/master/reference_guide/api_changes_list.md"
  }
}