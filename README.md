[![JetBrains team project](http://jb.gg/badges/team-plastic.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
# intellij-plugin-verifier

[ ![Download](https://api.bintray.com/packages/jetbrains/intellij-plugin-service/intellij-plugin-verifier/images/download.svg) ](https://bintray.com/jetbrains/intellij-plugin-service/intellij-plugin-verifier/_latestVersion)


intellij-plugin-verifier is a tool that can be used to check the binary compatibility between IntelliJ IDE builds and 
IntelliJ-platform plugins.

# Installation

Download a *jar* containing the `PluginVerifierMain` and all necessary dependencies from [Bintray](https://bintray.com/jetbrains/intellij-plugin-service/intellij-plugin-verifier)

## curl

\<version\> - the latest version of the intellij-plugin-verifier (see at the top)

`curl -L --output verifier-all.jar https://dl.bintray.com/jetbrains/intellij-plugin-service/org/jetbrains/intellij/plugins/verifier-cli/<version>/verifier-cli-<version>-all.jar`

# Usage

## Terms

__IDE__ - a particular IDE build to be verified 

__plugin id__ - the unique plugin identifier as specified in META-INF/plugin.xml

__plugin build__ - a particular version of the plugin 

__compatible__ plugin - IDE version is in range of the plugin's _\[since; until\]_ of META-INF/plugin.xml  

__last__ plugin build - the most recent plugin build *compatible* with IDE

## Options

`java -jar verifier-all.jar [Command] [Options]`

Command is either `check-plugin` or `check-ide`

#### check-plugin

```
syntax: check-plugin <plugin> <IDE path #1> <IDE path #2> ... <IDE path #N>
            [-runtime-dir | -r <file>]
            [-team-city | -tc ]
            [-tc-grouping | -g ]
            [-html-report <file>]
            [-external-prefixes <':'-separated list>]
            
<plugin> = <path to plugin> | #<plugin build number> | @<file containing multiple plugin paths>            
```

#### check-ide

```
syntax: check-ide <IDE containing directory> 
            [-runtime-dir | -r <file>] 
            [-plugins-to-check-file | -ptcf <file>]
            [-plugins-to-check-all-builds | -p-all < ':'-separated list>] 
            [-plugins-to-check-last-builds | -p-last < ':'-separated list>] 
            [-excluded-plugins-file | -epf <file> ] 
            [-html-report <file>]
            [-team-city | -tc ]
            [-tc-grouping | -g ]
            [-external-prefixes <':'-separated list>]
            [-dump-broken-plugin-list | -d]
```
If no plugins are explicitly specified then all compatible plugins in the [Plugin Repository](https://plugins.jetbrains.com) will be verified

#### Options description

```
-runtime-dir
    The path to directory containing Java runtime jar.
    If not specified, the JDK from 'JAVA_HOME' will be chosen.

-html-report 
    File to save the verification report in presentable form.

-external-prefixes 
    The plugin-verifier will not treat such classes as missing.

-plugins-to-check-all-builds 
    Verify ALL compatible builds of the specified plugin ids.

-plugins-to-check-last-builds 
    Verify LAST compatible builds of the specified plugin ids.

-team-city 
    Specify this flag if you want to print the TeamCity compatible output on stdout.

-tc-grouping 
    Group the TeamCity presentation of the problems:
    either 'plugin' to group by each plugin or 'problem_type' to group by problem type.

-excluded-plugins-file 
    File with list of excluded plugin builds. 
    The verifier will not verify such updates even if they are compatible with IDE.

-dump-broken-plugin-list 
    File to dump broken plugin ids. 
    The broken plugins are those which contain at least one problem as a result of the verification.

-plugins-to-check-file 
    Each line of the file is either '<plugin_id>' (check ALL builds of the plugin) 
    or '@<plugin_id>' (check only LAST build of the plugin).

-plugins-to-check-file 
    The file containing plugin ids to be verified separated by newline.
    If the id is prepended by '$' then only the last plugin build will be verified
```


## Examples

##### Check IDEA Ultimate #162.1121.32 

`java -jar verifier-all.jar -runtime-dir /usr/lib/jvm/java-8-oracle -html-report report.html -team-city -tc-grouping problem_type -excluded-plugins-file ignorePlugins.txt -plugins-to-check-file pluginsToCheck.txt -dump-broken-plugin-list actualBroken.txt check-ide /tmp/IU-162.1121.32`

#### Check IDEA Ultimate #162.1121.32 with Kotlin*, NodeJs* and PHP

`java -jar verifier-all.jar -runtime-dir /usr/lib/jvm/java-8-oracle -plugins-to-check-all-builds org.jetbrains.kotlin:NodeJS -plugins-to-check-last-builds com.jetbrains.php check-ide /tmp/IU-162.1121.32`

#### Check Kotlin plugin with IDEA Ultimate #162.2032.8, #163.1024, #163.7277

`java -jar verifier-all.jar -runtime-dir /usr/lib/jvm/java-8-oracle -external-prefixes org.custom.prefix:org.custom.other.prefix check-plugin /tmp/Kotlin /tmp/IU-162.2032.8 /tmp/IU-163.1024 /tmp/IU-163.7277`