# IntelliJ Plugin Verifier

[![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![X Follow](https://img.shields.io/badge/follow-%40JBPlatform-1DA1F2?logo=x)](https://twitter.com/JBPlatform)
[![Slack](https://img.shields.io/badge/Slack-%23intellij--plugin--verifier-blue)](https://plugins.jetbrains.com/slack)

[![IDE Diff Builder](https://github.com/JetBrains/intellij-plugin-verifier/actions/workflows/ide-diff-builder.yml/badge.svg)](https://github.com/JetBrains/intellij-plugin-verifier/actions/workflows/ide-diff-builder.yml)
[![IntelliJ Feature Extractor](https://github.com/JetBrains/intellij-plugin-verifier/actions/workflows/intellij-feature-extractor.yml/badge.svg)](https://github.com/JetBrains/intellij-plugin-verifier/actions/workflows/intellij-feature-extractor.yml)
[![IntelliJ Plugin Structure](https://github.com/JetBrains/intellij-plugin-verifier/actions/workflows/intellij-plugin-structure.yml/badge.svg)](https://github.com/JetBrains/intellij-plugin-verifier/actions/workflows/intellij-plugin-structure.yml)
[![IntelliJ Plugin Verifier](https://github.com/JetBrains/intellij-plugin-verifier/actions/workflows/intellij-plugin-verifier.yml/badge.svg)](https://github.com/JetBrains/intellij-plugin-verifier/actions/workflows/intellij-plugin-verifier.yml)
[![Plugins Verifier Service](https://github.com/JetBrains/intellij-plugin-verifier/actions/workflows/plugins-verifier-service.yml/badge.svg)](https://github.com/JetBrains/intellij-plugin-verifier/actions/workflows/plugins-verifier-service.yml)

IntelliJ Plugin Verifier checks the binary compatibility between IntelliJ-based IDE builds and IntelliJ Platform plugins.

This tool is useful because plugin authors often specify a wide [\[since; until\] compatibility range](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html##idea-plugin__idea-version) but compile a plugin against only a specific IDE from the range.
The IntelliJ Platform API can occasionally change between releases, so binary incompatibilities may arise, leading to `NoClassDefFoundError`, `NoSuchMethodError`, and similar exceptions at runtime.

> [!TIP]
> In most cases, IntelliJ Plugin Verifier will be used via `verifyPlugin` (2.x) / `runPluginVerifier` (1.x) task from Gradle plugin, see [Integration](#integration).

Example problems the Plugin Verifier can detect:

1) Plugin references a class `com.example.Foo`, which is not available in the IDE. This can happen if the plugin was compiled against IDE v1.0, and the class `com.example.Foo` was removed in IDE v2.0.
2) Plugin references a missing method of the IDE's class, which leads to `NoSuchMethodError` at runtime.
3) Many other binary incompatibilities as listed in [Java Specification | Binary Compatibility](https://docs.oracle.com/javase/specs/jls/se9/html/jls-13.html).
4) Missing plugin dependencies, for example, when plugin `A` depends on plugin `B`, but plugin `B` doesn't have a build that’s compatible with this IDE. It means that the user cannot install plugin `A` as the IDE requires all dependent plugins to be installed.

## Table of Contents

- [Installation](#installation)
- [Options](#options)
- [Results](#results)
- [Commands](#commands)
    - [check-ide](#check-ide)
    - [check-plugin](#check-plugin)
    - [check-trunk-api](#check-trunk-api)
    - [Common Options](#common-options)
- [Technical details](#technical-details)
- [Integration](#integration)
- [Feedback](#feedback)

## Installation

Download the latest available `verifier-cli-<version>-all.jar` from the [JetBrains Package Repository](https://packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-verifier/org/jetbrains/intellij/plugins/verifier-cli/) or from [Maven Central](https://repo1.maven.org/maven2/org/jetbrains/intellij/plugins/verifier-cli/).

As an alternative, use `curl` to download the JAR archive from the command-line:

    curl -L -o verifier-all.jar https://packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-verifier/org/jetbrains/intellij/plugins/verifier-cli/<version>/verifier-cli-<version>-all.jar

The `<version>` is the latest version, which you can find in the package [page](https://packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-verifier/org/jetbrains/intellij/plugins/verifier-cli/) or in the [GitHub Releases](https://github.com/JetBrains/intellij-plugin-verifier/releases).

You can use the GitHub API to retrieve the most recent release information. 
The resulting JSON response can be parsed using the `jq` tool. 
Following this, the artifact URL can be supplied to `curl` for download.

    curl -s https://api.github.com/repos/JetBrains/intellij-plugin-verifier/releases/latest \
        | jq -r '.assets[].browser_download_url' \
        | xargs curl -L --output verifier-all.jar

## Options

The Plugin Verifier can be run using the command line:

    java -jar verifier-all.jar [Command] [Options]

Command is one of `check-plugin`, `check-ide` or `check-trunk-api`.

Beginning with version 1.260, the Plugin Verifier requires Java 11.
Prior to this version, Java 8 is required.

## Results

All the verification results are printed and saved in the following ways:

1) The results are saved to `<verification-$timestamp>` directory (can be changed with `-verification-reports-dir` [option](#common-options)).
   The layout of files beneath this directory is as follows. The format of individual files is not specified.
   Basically, the files contain human-readable sentences.

       <verification reports dir>/
           <IDE version #1>/
               plugins/
                   <ID of plugin #1/
                       <Version of plugin #1>/
                           <... report files ...>
                   <ID of plugin #2>/
                       ...
           <IDE version #2>/
               plugins/
                   ...

   | **File**                        | **Description**                                                                    | **Exists if**     |
   | ------------------------------- | ---------------------------------------------------------------------------------- | ----------------- |
   | `verification-verdict.txt`      | Human-readable verification verdict.                                               | Always            |
   | `dependencies.txt`              | Dependencies of the plugin used during verification.                               | Plugin is valid   | 
   | `compatibility-warnings.txt`    | Compatibility warnings of this plugin with the IDE.                                | `> 0`             |
   | `compatibility-problems.txt`    | Compatibility problems of this plugin with the IDE.                                | `> 0`             |
   | `deprecated-usages.txt`         | Descriptions of "Deprecated API is used" cases.                                    | `> 0`             |
   | `experimental-api-usages.txt`   | Descriptions of "Experimental API is used" cases.                                  | `> 0`             |
   | `internal-api-usages.txt`       | Descriptions of "Internal API is used" cases.                                      | `> 0`             |
   | `override-only-usages.txt`      | Descriptions of "Override-only API is used incorrectly" cases.                     | `> 0`             |
   | `non-extendable-api-usages.txt` | Descriptions of "Non-extendable API is used incorrectly" cases.                    | `> 0`             |
   | `plugin-structure-warnings.txt` | Descriptions of plugin's own problems, which are not related to IDE compatibility. | `> 0`             |
   | `invalid-plugin.txt`            | Description of the invalid plugin error, in case the plugin is invalid.            | Plugin is invalid |
   
   **Note!** If you are implementing integration with the Plugin Verifier, you may check the presence of corresponding files to distinguish "successful" and "failed" verifications.

2) If the `-teamcity (-tc)` option is specified, the results are printed in [TeamCity Tests Format](https://www.jetbrains.com/help/teamcity/service-messages.html#Reporting+Tests).
   To choose a presentation type, specify the `-tc-grouping (-g)` option to either `plugin`, to group by each plugin, or `problem_type`, to group by the problem.
3) If `-teamcity` isn't specified, the results are printed to console.

## Commands

### check-ide

This command is used to check IDE build against a set of plugins.

    check-ide
        <IDE>
        [-runtime-dir | -r <file>]
        [-plugins-to-check-file | -ptcf <file>]
        [-plugins-to-check-all-builds | -p-all < ':'-separated list>]
        [-plugins-to-check-last-builds | -p-last < ':'-separated list>]
        [-excluded-plugins-file | -epf <file> ]
        [-team-city | -tc ]
        [-tc-grouping | -g ]
        [-external-prefixes <':'-separated list>]
        [-dump-broken-plugin-list | -d]
        [-ignored-problems | -ip <file>]
        [-keep-only-problems | -kop <file>]

`<IDE>` is either a path to local IDE installation, or an IDE pattern (see below in the [common options](#common-options)).

If no plugins are explicitly specified, then all compatible plugins in the [Plugin Repository](https://plugins.jetbrains.com) will be verified ([options](#common-options)).

#### Examples

Check IDEA Ultimate #162.1121.32 against all plugins listed in `pluginsToCheck.txt`:

    java -jar verifier-all.jar -runtime-dir /home/user/.jdks/corretto-11.0.8 -team-city -tc-grouping problem_type -excluded-plugins-file ignorePlugins.txt -plugins-to-check-file pluginsToCheck.txt -dump-broken-plugin-list actualBroken.txt check-ide /tmp/IU-162.1121.32

Check IDEA Ultimate 162.1121.32 against all versions of `Kotlin` and `NodeJs` plugins and the last version of the `PHP` plugin:

    java -jar verifier-all.jar -runtime-dir /home/user/.jdks/corretto-11.0.8 -plugins-to-check-all-builds org.jetbrains.kotlin:NodeJS -plugins-to-check-last-builds com.jetbrains.php check-ide /tmp/IU-162.1121.32

### check-plugin

This command is used to check one or more plugins against one or more IDEs ([options](#common-options)).

    check-plugin
        <plugins>
        <IDE> [<IDE>]*
        [-runtime-dir | -r <file>]
        [-team-city | -tc ]
        [-tc-grouping | -g ]
        [-external-prefixes <':'-separated list>]
        [-suppress-internal-api-usages no|jetbrains-plugins]
        [-mute comma-separated plugin problem identifier list]

`<plugins>` is either `<plugin path>` or `'@<file>'` with a list of plugin paths to verify, separated by a newline.

`<IDE>` is either a path to local IDE installation, or an IDE pattern (see below in the [common options](#common-options)). 

#### Specific options

* `-suppress-internal-api-usages` will suppress internal API usages from JetBrains plugins.
  This option is used by JetBrains Marketplace by default.

  Allowed values:

    * `no`: all internal API usages will be reported. This is the default value.
    * `jetbrains-plugins`: internal API usages by JetBrains plugins will not be reported.

* `-mute` will mute (ignore) a specified plugin problems.

  Supported values:

    - `ForbiddenPluginIdPrefix`,
    - `TemplateWordInPluginId`,
    - `TemplateWordInPluginName`,
    - `ReleaseVersionAndPluginVersionMismatch`,

  A comma-separated list of plugin problems is allowed, e.g.:

        -mute TemplateWordInPluginId,TemplateWordInPluginName

  The switch will mute any kind of supported plugin problems —
  including plugin problems related to the plugin descriptor.

  Usually, a long-existing plugin uploaded to the JetBrains Marketplace might be verified
  with more relaxed rules than a new plugin.
  This option is used to mute plugin problems that do not apply to such a plugin.

#### Plugin Path Specification

Plugin path may be specified in multiple formats:

- `id:<plugin-id>` indicates all compatible versions of _plugin-id_. Example: `id:training` denotes all versions of _IDE Features Trainer_ plugin.
- `version:<plugin-id>:<version>` points to a specific version of _plugin-id_. Example: `version:training:231.8770.31` denotes a specific version of _IDE Features Trainer_ plugin.
- `#<update-id>` identifies a specific plugin update identifier. Such `update-id` is coupled with a plugin ID and a plugin version.
- `path:<path>` identifies a plugin by the path to the distribution file (usually a ZIP) in the filesystem.
- `identifier` without any specification is resolved either as a `path:` suffix or `id:` suffix.
- `@<file>` points to a file in a local filesystem. This file contains a list of plugin paths, separated by newline. Each line adheres to any format specified above (`id`, `version` etc.).

#### Examples

Check `Kotlin` plugin against IDEA Ultimate 162.2032.8, 163.1024, and 163.7277:

    java -jar verifier-all.jar -runtime-dir /home/user/.jdks/corretto-11.0.8 check-plugin /tmp/Kotlin /tmp/IU-162.2032.8 /tmp/IU-163.1024 /tmp/IU-163.7277

Check an individual plugin packaged as a ZIP file against IDEA Ultimate 162.2032.8 on macOS:

     java -jar verifier-all.jar check-plugin ~/counter/build/distributions/counter-12.0.0.zip /tmp/IU-162.2032.8/Contents

Check all versions of plugin with ID `training` against IDEA Ultimate 162.2032.8 and PyCharm PC-203.7717.81 on macOS:

    java -jar verifier-all.jar check-plugin training /tmp/idea-edu-2022.2.2/Contents /tmp/PC-203.7717.81.app/Contents

Check all versions of plugin with ID `training` and version `231.8770.31` against IDEA Ultimate 162.2032.8 and PyCharm PC-203.7717.81 on macOS:

    java -jar verifier-all.jar check-plugin version:training:231.8770.31 /tmp/idea-edu-2022.2.2/Contents /tmp/PC-203.7717.81.app/Contents

Check a specific plugin update by its ID against IDEA Ultimate 162.2032.8 and PyCharm PC-203.7717.81 on macOS. The update `#323705` corresponds to the plugin `training` with version version `231.8770.31`.

    java -jar verifier-all.jar check-plugin '#323705' /tmp/idea-edu-2022.2.2/Contents tmp/PC-203.7717.81.app/Contents

Note that the update ID is quoted to prevent shell mangling.

Check a list of plugins specified in a plaintext file `jetbrains-plugins.txt` against IDEA Ultimate 162.2032.8 and PyCharm PC-203.7717.81 on macOS.

    java -jar verifier-all.jar check-plugin @jetbrains-plugins.txt /tmp/idea-edu-2022.2.2/Contents /tmp/PC-203.7717.81.app/Contents

The file `jetbrains-plugins.txt` might contain two plugin paths on separate lines.

    version:com.intellij.quarkus:231.8770.17
    #320655

### check-trunk-api

This command tracks API Changes between two IDE builds: a **release** and a **trunk**.

Note that its purpose is to detect incompatibilities between two IDE builds, not to detect *all* the plugins' own problems.

Given the release IDE build, all plugins' versions for release IDE will be verified with both the release and trunk and only new problems with a trunk will be reported.

For clarity, here is an example of the command:

    check-trunk-api
        -r /home/user/.jdks/corretto-11.0.8
        -subsystems-to-check without-android
        -team-city
        -jetbrains-plugins-file all-jetbrains-plugins.txt
        -release-jetbrains-plugins release-plugins
        -trunk-jetbrains-plugins trunk-plugins
        -major-ide-path IU-173.4548.28
        IU-181.3741.2

The `IU-173.4548.28` is IDEA Ultimate 2017.3.4 build, and `IU-181.3741.2` is some IDE built from the master.
This command will do the following:

1) Take all plugins from the Plugin Repository compatible with `IU-173.4548.28` and run the verification against `IU-173.4548.28`.
2) Take the same versions of the plugins and verify them against `IU-181.3741.2`, even if those plugins' \[since; until\] compatibility ranges don't include the `IU-181.3741.2`.
3) Report problems that are present in `IU-181.3741.2` but not present in `IU-173.4548.28`.

There are the following points to mention:

1) IntelliJ API is considered to consist of all the classes bundled to IDE, all its bundled plugins, and all the JetBrains-developed plugins compiled from the same sources revision.
   Those plugins are not bundled into IDE distribution but are available locally after the IDE build finishes.
   The plugins are typically uploaded to the Plugin Repository when a new release IDE gets out, but it cannot be valid for intermediate builds.
   * `-jetbrains-plugins-file all-jetbrains-plugins.txt` points to a file containing IDs of all JetBrains plugins built from the same sources as the IDE, like (`NodeJS`, `Pythonid`, `Docker`).
   * `-release-jetbrains-plugins release-plugins` points to a directory containing all the plugins built along with the **release** IDE.
     Plugins can be in the form of `.jar`, `.zip`, or directories.
   * `-trunk-jetbrains-plugins trunk-plugins` points to a directory containing all the plugins built along with the **trunk** IDE.
2) `-subsystems-to-check without-android` specifies that the Plugin Verifier should not show problems related to Android support.

Here is the full syntax of the command:

    check-trunk-api <trunk IDE>
        [-runtime-dir | -r <file>]
        [-major-ide-path | -mip <file>]
        [-external-prefixes <':'-separated list>]
        [-subsystems-to-check | -subsystems]
        [-release-jetbrains-plugins | -rjbp <path>]
        [-trunk-jetbrains-plugins | -tjbp <path>]
        [-team-city | -tc ]

#### Specific options

* `-major-ide-path (-mip)`

    The path to the major IDE release build to compare API problems of the trunk (master) IDE build.

* `-release-jetbrains-plugins (-rjbp)`

    The root of the local plugin repository containing JetBrains plugins compatible with the release IDE.
    The local repository is a set of non-bundled JetBrains plugins built from the same sources revision.
    The verifier will read the plugin descriptors from every plugin-like file under the specified directory.
    During the verification, the JetBrains plugins will be taken from the local repository, if present.
    Otherwise, they will be fetched from the public repository.

* `-trunk-jetbrains-plugins (-tjbp)`
    The same as `--release-local-repository` but specifies the directory containing plugins built for the trunk IDE.

### Common Options

* `<IDE>`
  
    The path to a local IDE installation or a pattern in form `[latest-release-IU]` or `[latest-IU]` (latest EAP).
    
    In the latter case the IDE will be downloaded to a temp directory `<temp dir>/<IDE version>`. You can change the
    `<temp dir>` part with `-Dintellij.plugin.verifier.download.ide.temp.dir=<custom path>` system property.

* `-verification-reports-dir (-vrd)`

    The path to the directory where verification reports will be saved.
    By default, it is equal to `<current working dir>/verification-<timestamp>`.

* `-verification-reports-formats (-vrf)`

    The output format of the verification reports. 
    Supported formats are: `plain` (console output), `html` and `markdown`
    By default, `plain` and `html` output formats are enabled.
    Multiple output formats are supported, separated by a comma.

    Output format that starts with a `-` (dash) will be suppressed: either from the default 
    set of output formats or from the specified output formats.

    Examples:

    * `plain,markdown` will enable console output and the Markdown verification reports.
    * `-plain` will disable console output, but retain the default HTML output.
    * `""` (literal empty string) will disable all verification report formats. 
    This will essentially suppress console output, leaving only logging messages.
    

* `-runtime-dir (-r)`

    The path to the directory containing Java runtime JAR files (JDK).
    If not specified, the embedded JDK from the provided `IDE` parameter will be used.  
    If the IDE does not contain an embedded JDK, the `JAVA_HOME` environment variable will be used to resolve the Java runtime.

* `-external-prefixes (-ex-prefixes)`

    The prefixes of classes from the external libraries.
    The Plugin Verifier will not report 'No such class' for classes of these packages.

* `-plugins-to-check-all-builds (-p-all)`

    The plugin IDs to check with the IDE.
    The plugin verifier will check ALL compatible plugin builds.

* `-plugins-to-check-last-builds (-p-last)`

    The plugin IDs to check with the IDE.
    The plugin verifier will check the LAST plugin build only.

* `-team-city (-tc)`

    Specify this flag if you want to print the TeamCity compatible output on stdout.

* `-tc-grouping (-g)`

    Group the TeamCity presentation of the problems:
    either 'plugin' to group by each plugin or 'problem_type' to group by problem type.

* `-excluded-plugins-file (-epf)`

    File with a list of excluded plugin builds.
    The verifier will not verify such updates even if they are compatible with the IDE.
    File with list of excluded plugin builds (e.g. '<IDE-home>/lib/resources.jar/brokenPlugins.txt').

* `-offline`

    Specify this flag if the Plugin Verifier must use only locally downloaded dependencies of plugins and must avoid making HTTP requests. 

* `-dump-broken-plugin-list (-d)`

    File to dump broken plugin ids.
    The broken plugins are those which contain at least one problem as a result of the verification.

* `-plugins-to-check-file (-ptcf)`

    A file that contains a list of plugins to check.
    Each line of the file is either:
    * `plugin_id` (check ALL builds of the plugin)
    * `$plugin_id'` (check only the LAST build of the plugin)

* `-subsystems-to-check (-subsystems)`

    Specifies which subsystems of IDE should be checked.
    Available options: `all` (default), `android-only`, `without-android`.

* `-ignored-problems (-ip)`

    A file that contains a list of problems that will be ignored in the report. 
    The file must contain lines in the format `<plugin_xml_id>:<plugin_version>:<problem_description_regexp_pattern>`.
    Both `plugin_xml_id` and `plugin_version` are optional.

    Examples:
 
    * `DevKit:242.19890.14:access to unresolved class org.foo.Foo.*` — ignore the plugin problem that occurs in the plugin with the identifier `DevKit` and version `242.19890.14`.   
    * `org.jetbrains.kotlin::access to unresolved class org.jetbrains.kotlin.compiler.*` — ignore the plugin problem for all versions of the Kotlin plugin.  
    * `access to unresolved class org.jetbrains.kotlin.compiler.*` — ignore the plugin problem for all plugins.

* `-keep-only-problems (-kop)`

    A file that contains patterns of problems that will be reflected in report. All other problems will be ignored. Applied to short problem description.
    The file must contain lines in form: `<plugin_xml_id_regexp_pattern>:<plugin_version_regexp_pattern>:<problem_description_regexp_pattern>`

## Technical details

Plugin Verifier uses the following paths for operational purposes:

* `<home-directory>` - base directory for all other directories:
    * By default, it is `<USER_HOME>/.pluginVerifier`,
    * It can be modified via `-Dplugin.verifier.home.dir` JVM parameter, e.g. `-Dplugin.verifier.home.dir=/tmp/verifier`,
* `<plugins-directory> = <home-directory>/loaded-plugins` - cache directory for downloaded plugins,
* `<extracted-directory> = <home-directory>/extracted-plugins` - temporary directory used for extracting plugins that are distributed as `.zip` archives.

**Downloading plugins**

Plugins to be verified and plugins' dependencies are downloaded into `<plugins-directory>`.
It can be reused between multiple runs of the Plugin Verifier: on the first run, all the necessary plugins will be downloaded, and on the subsequent runs, they will be taken from the cache.
Note that not only the verified plugins are downloaded but also all plugins' dependencies.

Plugins are downloaded from the [Plugin Repository](https://plugins.jetbrains.com/) into  `<plugins-directory>/<update-ID>.jar` or `<plugins-directory>/<update-ID>.zip`, depending on the plugin's packaging type.
`<update-ID>` is the unique ID of the plugin's version in the Plugin Repository's database.
For example, [Kotlin 1.2.30-release-IJ2018.1-1](https://plugins.jetbrains.com/plugin/6954-kotlin/update/43775) has `update-ID` equal to `43775`.

**Limit size of `<plugins-directory>`**

That's possible to limit the size of the `<plugins-directory>`, which is 5 GB by default.
To do this, specify JVM option `-Dplugin.verifier.cache.dir.max.space=<max-space-MB>`.
The Plugin Verifier will remove the least recently used plugins from the cache as soon as the occupied space reaches the limit.

**Extracting .zip-ed plugins**

Plugins packaged in `.zip` archives are extracted into `<extracted-directory>/<temp-dir>` before verifying these plugins starts.
This is necessary to speed up the verification, which needs to do many searches of class-files.

## Integration

The most straightforward way of integrating the Plugin Verifier with your project is using dedicated Gradle task:

- IntelliJ Platform Gradle Plugin (2.x): [`verifyPlugin`](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#verifyPlugin)
- Gradle IntelliJ Plugin (1.x): [`runPluginVerifier`](https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#tasks-runpluginverifier)

If you're not using Gradle within your project, there are predefined third-party actions available in the GitHub Actions Marketplace that automate the plugin verification process.

Read more about possible integration options: [Verifying Plugin Compatibility](https://plugins.jetbrains.com/docs/intellij/verifying-plugin-compatibility.html)

## Feedback

Please report issues to YouTrack: https://youtrack.jetbrains.com/issues/MP (MP stands for `Marketplace`)

- Check if there is already a similar ticket present.
- If not, create a [<kbd>New Issue</kbd>](https://youtrack.jetbrains.com/newIssue?project=MP&c=Subsystem%20Plugin%20Verifier).
- Type the issue **Summary** and **Description**.
- Select **Subsystem** to be **Plugin Verifier** - YouTrack will automatically assign a responsible developer.
- If the issue is a feature request, you may select **Type** to be **Feature**.

*Thank you in advance for reporting issues, providing feedback, and making the Plugin Verifier better!*

### Slack

There is also a dedicated Slack channel available: [#intellij-plugin-verifier](https://plugins.jetbrains.com/slack).

The [JetBrains Platform Slack](https://plugins.jetbrains.com/slack) community is a place where you can talk with other plugin developers and JetBrains employees about plugin and extension development.
