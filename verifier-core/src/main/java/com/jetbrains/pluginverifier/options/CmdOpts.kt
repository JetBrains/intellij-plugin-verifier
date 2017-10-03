package com.jetbrains.pluginverifier.options

import com.sampullara.cli.Argument

open class CmdOpts(
    @set:Argument("ignored-problems", alias = "ip", description = "The problems specified in this file will be ignored. The file must contain lines in form <plugin_xml_id>:<plugin_version>:<problem_description_regexp_pattern>")
    var ignoreProblemsFile: String? = null,

    @set:Argument("save-ignored-problems-to-file", alias = "siptf", description = "The problems listed in the --ignored-problems file will be ignored from the main report but printed into the specified file.")
    var saveIgnoredProblemsFile: String? = null,

    @set:Argument("ide-version", alias = "iv", description = "The actual version of the IDE that will be verified. This value will overwrite the one found in the IDE itself")
    var actualIdeVersion: String? = null,

    @set:Argument("external-classpath", alias = "ex-cp", delimiter = ":", description = "The classes from external libraries. The Verifier will not report 'No such class' for such classes.")
    var externalClasspath: Array<String> = arrayOf(),

    @set:Argument("ignore-all-missing-optional-dependencies", alias = "ignore-all-missing-opt-deps", description = "If specified, all the optional missing plugins will not be treated as problems")
    var ignoreAllMissingOptionalDeps: Boolean = false,

    @set:Argument("ignore-specific-missing-optional-dependencies", alias = "ignore-specific-missing-opt-deps", delimiter = ":")
    var ignoreMissingOptionalDeps: Array<String> = arrayOf(),

    @set:Argument("documented-problems-url", alias = "dpu", description = "The URL of the page containing documented problems that must not be reported. " +
        "By default it is $DEFAULT_DOCUMENTED_PROBLEMS_PAGE_URL that contains the sources of the page of " +
        "SDK Documentation: http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html")
    var documentedProblemsPageUrl: String? = DEFAULT_DOCUMENTED_PROBLEMS_PAGE_URL

) : PublicOpts() {
  companion object {
    const val DEFAULT_DOCUMENTED_PROBLEMS_PAGE_URL = "https://raw.githubusercontent.com/JetBrains/intellij-sdk-docs/master/reference_guide/api_changes_list.md"
  }
}