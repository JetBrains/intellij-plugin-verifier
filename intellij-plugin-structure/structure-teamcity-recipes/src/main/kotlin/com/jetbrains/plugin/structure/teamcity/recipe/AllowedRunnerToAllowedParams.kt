package com.jetbrains.plugin.structure.teamcity.recipe

val allowedRunnerToAllowedParams = mapOf(
  "gradle" to setOf(
    "tasks",
    "build-file",
    "incremental",
    "working-directory",
    "gradle-home",
    "gradle-params",
    "use-gradle-wrapper",
    "gradle-wrapper-path",
    "enable-debug",
    "enable-stacktrace",
    "jdk-home",
    "jvm-args",
  ),
  "maven" to setOf(
    "goals",
    "pom-location",
    "runner-arguments",
    "working-directory",
    "path",
    "user-settings-selection",
    "user-settings-path",
    "use-own-local-repo",
    "local-repo-scope",
    "is-incremental",
    "jdk-home",
    "jvm-args",
  ),
  "node-js" to setOf(
    "working-directory",
    "shell-script",
  ),
  "command-line" to setOf(
    "use-custom-script",
    "script-content",
    "working-directory",
    "format-stderr-as-error",
    "path",
    "arguments",
  ),
)