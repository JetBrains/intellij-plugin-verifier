[![official JetBrains project](https://jb.gg/badges/official-flat-square.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
# intellij-plugin-verifier

[ ![Download](https://api.bintray.com/packages/jetbrains/intellij-plugin-service/intellij-plugin-verifier/images/download.svg) ](https://bintray.com/jetbrains/intellij-plugin-service/intellij-plugin-verifier/_latestVersion)


IntelliJ Plugin Verifier is used to check binary compatibility between IntelliJ IDE builds and 
IntelliJ Platform plugins.

This tool is useful because plugins authors' often specify wide [[since; until] compatibility range](http://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_configuration_file.html)
but compile a plugin against only a specific IDE from the range. IntelliJ API may be occasionally changed between releases,
so binary incompatibilities may arise, leading to `NoClassDefFoundError`, `NoSuchMethodError` and similar exceptions at runtime.

Examples of problems that the Plugin Verifier is able to detect:
1) Plugin references a class `com.example.Foo` which is not available in IDE.
It may happen if the plugin had been compiled against IDE 1.0, and the class `com.example.Foo` was removed in IDE 2.0.
2) Plugin references a missing method of IDE's class, which leads to `NoSuchMethodError` at runtime.
3) Many other binary incompatibilities listed in [Java Spec. on Binary Compatibility](https://docs.oracle.com/javase/specs/jls/se9/html/jls-13.html)   
4) Missing plugin's dependencies problems: a plugin `A` depends on another plugin `B` that doesn't have a build compatible with this IDE: 
it means that the user cannot install the plugin `A` at all, as the IDE requires all dependent plugins to be installed.

# Usage

## Installation

Download self-contained *verifier-all.jar* from [Bintray](https://bintray.com/jetbrains/intellij-plugin-service/intellij-plugin-verifier),
or use the below `curl` script:

    curl -L --output verifier-all.jar https://dl.bintray.com/jetbrains/intellij-plugin-service/org/jetbrains/intellij/plugins/verifier-cli/<version>/verifier-cli-<version>-all.jar

where \<version\> is the latest released version (see at the top).

## Options

The Plugin Verifier can be run using the command line: 

    java -jar verifier-all.jar [Command] [Options]

Command is one of `check-plugin`, `check-ide` or `check-trunk-api`.

## Results

All the verification results are printed and saved in the following ways:
  1) The results are saved to `<verification-$timestamp>` directory (can be changed with `-verification-reports-dir` [option](#common-options)).
     The layout of files beneath this directory is as follows. Format of individual files is not specified. Basically, the files contain human-readable sentences.

    <verification reports dir>/
        <IDE version #1>/
            plugins/
                <ID of plugin #1/
                    <Version of plugin #1>/
                        <... report files ...>


| **File** | **Description** | **Exists if** |
| -------- | --------------- | --------- |
| `verification-verdict.txt` | Human-readable verification verdict. | Always |
| `dependencies.txt` | Dependencies of the plugin used during verification. | Plugin is valid | 
| `compatibility-warnings.txt` | Compatibility warnings of this plugin with the IDE #1. | `> 0` |
| `compatibility-problems.txt` | Compatibility problems of this plugin with the IDE #2. | `> 0` |
| `deprecated-usages.txt` | Descriptions of "Deprecated API is used" cases. | `> 0` |
| `experimental-api-usages.txt` | Descriptions of "Experimental API is used" cases. | `> 0` |
| `internal-api-usages.txt` | Descriptions of "Internal API is used" cases. | `> 0` |
| `override-only-usages.txt` | Descriptions of "Override-only API is used incorrectly" cases. | `> 0` |
| `non-extendable-api-usages.txt` | Descriptions of "Non-extendable API is used incorrectly" cases. | `> 0` |
| `plugin-structure-warnings.txt` | Descriptions of plugin's own problems, which are not related to IDE compatibility. | `> 0` |
| `invalid-plugin.txt` | Description of the invalid plugin error, in case the plugin is invalid. | Plugin is invalid |

**Note!** If you are implementing an integration with the Plugin Verifier, you may check the presence of corresponding files
to distinguish "successful" and "failed" verifications. 

  2) If `-teamcity (-tc)` option is specified, the results are printed in [TeamCity Tests Format](https://confluence.jetbrains.com/display/TCD10/Build+Script+Interaction+with+TeamCity#BuildScriptInteractionwithTeamCity-ReportingTests).
  To choose a presentation type specify the `-tc-grouping (-g)` option to either `plugin`, to group by each 
  plugin, or `problem_type`, to group by problem.
  3) If `-teamcity` isn't specified, the results are printed to console.

## Commands

### check-ide

This command is used to check IDE build against a set of plugins.

    check-ide 
        <IDE path> 
        [-runtime-dir | -r <file>] 
        [-plugins-to-check-file | -ptcf <file>]
        [-plugins-to-check-all-builds | -p-all < ':'-separated list>] 
        [-plugins-to-check-last-builds | -p-last < ':'-separated list>] 
        [-excluded-plugins-file | -epf <file> ] 
        [-team-city | -tc ]
        [-tc-grouping | -g ]
        [-external-prefixes <':'-separated list>]
        [-dump-broken-plugin-list | -d]

If no plugins are explicitly specified then all compatible plugins in the [Plugin Repository](https://plugins.jetbrains.com) will be verified ([options](#common-options)).

##### Examples

Check IDEA Ultimate #162.1121.32 against all plugins listed in `pluginsToCheck.txt`

    java -jar verifier-all.jar -runtime-dir /usr/lib/jvm/java-8-oracle -team-city -tc-grouping problem_type -excluded-plugins-file ignorePlugins.txt -plugins-to-check-file pluginsToCheck.txt -dump-broken-plugin-list actualBroken.txt check-ide /tmp/IU-162.1121.32

Check IDEA Ultimate 162.1121.32 against all version of `Kotlin` and `NodeJs` plugins and last version of `PHP` plugin

    java -jar verifier-all.jar -runtime-dir /usr/lib/jvm/java-8-oracle -plugins-to-check-all-builds org.jetbrains.kotlin:NodeJS -plugins-to-check-last-builds com.jetbrains.php check-ide /tmp/IU-162.1121.32

### check-plugin

This command is used to check one or more plugins against one or more IDEs ([options](#common-options)).

    check-plugin 
        <plugins> 
        <IDE path> [<IDE path>]*
        [-runtime-dir | -r <file>]
        [-team-city | -tc ]
        [-tc-grouping | -g ]
        [-external-prefixes <':'-separated list>]

        where <plugins> is either <plugin path> or '@<file>' with a list of plugins paths to verify, separated by newline.

##### Examples

Check `Kotlin` plugin against IDEA Ultimate 162.2032.8, 163.1024 and 163.7277.

    java -jar verifier-all.jar -runtime-dir /usr/lib/jvm/java-8-oracle check-plugin /tmp/Kotlin /tmp/IU-162.2032.8 /tmp/IU-163.1024 /tmp/IU-163.7277

### check-trunk-api

This command is used to track API Changes between two IDE builds: a **release** and a **trunk**.

Note that its purpose is to detect incompatibilities introduced between two IDE builds, not to detect *all* the plugins' own problems.

Given the release IDE build, all plugins' versions for release IDE will be verified with both the release and trunk
and only new problems with trunk will be reported.

For clarity, here is an example of the command:

    check-trunk-api 
       -r /usr/lib/jvm/java-8-oracle 
       -subsystems-to-check without-android
       -team-city 
       -jetbrains-plugins-file all-jetbrains-plugins.txt
       -release-jetbrains-plugins release-plugins
       -trunk-jetbrains-plugins trunk-plugins
       -major-ide-path IU-173.4548.28 
       IU-181.3741.2

The `IU-173.4548.28` is IDEA Ultimate 2017.3.4 build, and `IU-181.3741.2` is some IDE built from master.
This command will do the following:
1) Take all plugins from the Plugin Repository compatible with `IU-173.4548.28` and run the verification against `IU-173.4548.28`.
2) Take the same versions of the plugins and verify them against `IU-181.3741.2`, even if those plugins' [since; until] compatibility
ranges don't include the `IU-181.3741.2`.
3) Report problems that are present in `IU-181.3741.2` but not present in `IU-173.4548.28`.

There are the following points to mention:
1) IntelliJ API is consider to consist of all the classes bundled to IDE, all its bundled plugins and all 
the JetBrains-developed plugins compiled from the same sources revision. Those plugins are not bundled into IDE distribution, 
but are available locally after the IDE build finishes. The plugins are typically uploaded to the Plugin Repository when a new 
release IDE gets out, but for intermediate builds it can be not true. 
    * `-jetbrains-plugins-file all-jetbrains-plugins.txt` points to a file containing IDs 
    of all JetBrains plugins that get built from the same sources as the IDE, like (`NodeJS`, `Pythonid`, `Docker`).
    * `-release-jetbrains-plugins release-plugins` points to a directory containing all the plugins built along with the **release** IDE. 
     Plugins can be in form of `.jar`, `.zip` or directories.
    * `-trunk-jetbrains-plugins trunk-plugins` points to a directory containing all the plugins built along with the **trunk** IDE.
2) `-subystems-to-check without-android` specifies that the Plugin Verifier should not show problems related to Android support.    


Here is the full syntax of the command:

    check-trunk-api <trunk IDE>
        [-runtime-dir | -r <file>]
        [-major-ide-path | -mip <file>]
        [-major-ide-version | -miv <IDE version>]
        [-external-prefixes <':'-separated list>]
        [-subsystems-to-check | -subsystems]
        [-release-jetbrains-plugins | -rjbp <path>]
        [-trunk-jetbrains-plugins | -tjbp <path>]
        [-team-city | -tc ]
    
#### Specific options
* `-major-ide-path (-mip)`
    The path to release (major) IDE build with which to compare API problems of trunk (master) IDE build.
* `-major-ide-version (-miv)`
    The IDE version with which to compare API problems. 
    This IDE will be downloaded from the IDE Repository: https://www.jetbrains.com/intellij-repository/releases
* `-release-jetbrains-plugins (-rjbp)`
    The root of the local plugin repository containing JetBrains plugins compatible with the release IDE.
    The local repository is a set of non-bundled JetBrains plugins built from the same sources revision.
    The verifier will read the plugin descriptors from every plugin-like file under the specified directory.
    During the verification, the JetBrains plugins will be taken from the local repository, if present, and 
    from the public repository, otherwise.
* `-trunk-jetbrains-plugins (-tjbp)`
    The same as `--release-local-repository` but specifies the directory containing plugins built for the trunk IDE.

### Common Options

`-verification-reports-dir (-vrd)`
    The path to directory where verification reports will be saved. 
    By default, it is equal to `<current working dir>/verification-<timestamp>`.

`-runtime-dir (-r)`
    The path to directory containing Java runtime jar.
    If not specified, the JDK from 'JAVA_HOME' will be chosen.

`-external-prefixes (-ex-prefixes)`
    The prefixes of classes from the external libraries. 
    The Plugin Verifier will not report 'No such class' for classes of these packages.

`-plugins-to-check-all-builds (-p-all)`
    The plugin IDs to check with IDE. The plugin verifier will check ALL compatible plugin builds

`-plugins-to-check-last-builds (-p-last)`
    The plugin IDs to check with IDE. The plugin verifier will check LAST plugin build only

`-team-city (-tc)`
    Specify this flag if you want to print the TeamCity compatible output on stdout.

`-tc-grouping (-g)`
    Group the TeamCity presentation of the problems:
    either 'plugin' to group by each plugin or 'problem_type' to group by problem type.

`-excluded-plugins-file (-epf)` 
    File with list of excluded plugin builds. 
    The verifier will not verify such updates even if they are compatible with IDE.
    File with list of excluded plugin builds (e.g. '<IDE-home>/lib/resources.jar/brokenPlugins.txt')

`-dump-broken-plugin-list (-d)`
    File to dump broken plugin ids. 
    The broken plugins are those which contain at least one problem as a result of the verification.

`-plugins-to-check-file (-ptcf)`
    File that contains list of plugins to check 
    Each line of the file is either:
        plugin_id (check ALL builds of the plugin) 
        $plugin_id' (check only LAST build of the plugin)
        
`-subsystems-to-check (-subsystems)`
    Specifies which subsystems of IDE should be checked. 
    Available options: `all` (default), `android-only`, `without-android`.
        
        
## Technical details

Plugin Verifier uses the following paths for operational purposes:
* `<home-directory>` - base directory for all other directories  
    * By default it is `<USER_HOME>/.pluginVerifier`
    * It can be modified via `-Dplugin.verifier.home.dir` JVM parameter, e.g. `-Dplugin.verifier.home.dir=/tmp/verifier` 
* `<plugins-directory> = <home-directory>/loaded-plugins` - cache directory for downloaded plugins
* `<extracted-directory> = <home-directory>/extracted-plugins` - temporary directory used for extracting plugins that are distributed as `.zip` archives.

###### Downloading plugins

Plugins to be verified and plugins' dependencies are downloaded into `<plugins-directory>`.
It can be reused between multiple runs of the Plugin Verifier: on the first run
all the necessary plugins will be downloaded, and on the subsequent runs they will be taken from the cache.
Note that not only the verified plugins are downloaded but also all plugins' dependencies.

Plugins are downloaded from the [Plugin Repository](https://plugins.jetbrains.com/) into 
`<plugins-directory>/<update-ID>.jar` or `<plugins-directory>/<update-ID>.zip`, depending on the 
plugin's packaging type. `<update-ID>` is the unique ID of the plugin's version in the Plugin Repository's database.
For example, [Kotlin 1.2.30-release-IJ2018.1-1](https://plugins.jetbrains.com/plugin/6954-kotlin/update/43775) has `update-ID`
equal to `43775`.

###### Limit size of `<plugins-directory>`

That's possible to limit size of the `<plugins-directory>`, which is 5 GB by default.
To do this, specify JVM option `-Dplugin.verifier.cache.dir.max.space=<max-space-MB>`. 
The Plugin Verifier will remove the least recently used plugins from the cache as soon as the occupied space reaches the limit.

###### Extracting .zip-ed plugins

Plugins packaged in `.zip` archives are extracted into `<extracted-directory>/<temp-dir>` before the verification of
these plugins starts. This is necessary to speedup the verification, which needs to do a lot of searches of class-files.

## Integration

#### GitHub Actions

[GitHub Actions](https://help.github.com/en/actions) allow introducing automation to the GitHub repository and create
workflows to test, build and deploy the project.

There are 3rd party predefined actions available in the GitHub Actions Marketplace that allow automating the plugin
verification process. Such actions are wrappers for the IntelliJ Plugin Verifier and provide various ways of the
configuration:

- [IntelliJ Platform Plugin Verifier](https://github.com/marketplace/actions/intellij-platform-plugin-verifier)
- [IntelliJ Plugin Verifier](https://github.com/marketplace/actions/intellij-plugin-verifier)
