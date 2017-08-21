package com.jetbrains.pluginverifier.options

import com.sampullara.cli.Argument

open class PublicOpts(
    @set:Argument("runtime-dir", alias = "r", description = "The path to directory containing Java runtime jars (e.g. /usr/lib/jvm/java-8-oracle ")
    var runtimeDir: String? = null,

    @set:Argument("team-city", alias = "tc", description = "Specify this flag if you want to print the TeamCity compatible output on stdout.")
    var needTeamCityLog: Boolean = false,

    @set:Argument("tc-grouping", alias = "g", description = "How to group the TeamCity presentation of the problems: either 'plugin' to group by each plugin or 'problem_type' to group by problem type")
    var teamCityGroupType: String? = null,

    @set:Argument("plugins-to-check-all-builds", alias = "p-all", delimiter = ":", description = "The plugin ids to check with IDE. The plugin verifier will check ALL compatible plugin builds")
    var pluginToCheckAllBuilds: Array<String> = arrayOf(),

    @set:Argument("plugins-to-check-last-builds", alias = "p-last", delimiter = ":", description = "The plugin ids to check with IDE. The plugin verifier will check LAST plugin build only")
    var pluginToCheckLastBuild: Array<String> = arrayOf(),

    @set:Argument("excluded-plugins-file", alias = "epf", description = "File with list of excluded plugin builds (e.g. '<IDE-home>/lib/resources.jar/brokenPlugins.txt')")
    var excludedPluginsFile: String? = null,

    @set:Argument("dump-broken-plugin-list", alias = "d", description = "File to dump broken plugin ids. The broken plugins are those which contain at least one problem as a result of the verification")
    var dumpBrokenPluginsFile: String? = null,

    @set:Argument("html-report", description = "Create HTML report of broken plugins")
    var htmlReportFile: String? = null,

    @set:Argument("plugins-to-check-file", alias = "ptcf", description = "File that contains list of plugins to check (e.g. '<IDE-home>/lib/resources.jar/checkedPlugins.txt')")
    var pluginsToCheckFile: String? = null,

    @set:Argument("external-prefixes", alias = "ex-prefixes", delimiter = ":", description = "The prefixes of classes from the external libraries. The Verifier will not report 'No such class' for such classes.")
    var externalClassesPrefixes: Array<String> = arrayOf()
)