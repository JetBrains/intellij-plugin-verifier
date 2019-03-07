####What does this repository hold

This [repository](https://github.com/JetBrains/intellij-plugin-verifier/) holds 4 projects:
1) [intellij-plugin-structure](intellij-plugin-structure) - API for working with the IntelliJ Plugins and IntelliJ IDEs:
reading the plugins' descriptors (_plugin.xml_ files), reading class files, veryfying the plugins' structures.

2) [intellij-plugin-verifier](.) - a bytecode verification library used to verify the API binary compatibility of 
IntelliJ plugins and IDE builds

3) [intellij-feature-extractor](intellij-feature-extractor) - a library used to extract the plugins' additional features,
such as supported file extensions, configuration types etc.

4) [plugins-verifier-service](plugins-verifier-service) - an HTTP server responsible for:
   * Running the _intellij-plugin-verifier_ tool for
     plugins from the JetBrains Plugin Repository against a set of predefined IDEs and sending 
     the verification results for storage to the repository.
   * Running the _intellij-feature-extractor_ for plugins and sending the extracted features to the repository.
   * You can gain more info [here](https://confluence.jetbrains.com/display/PLREP/plugin-verifier+integration+with+the+plugins.jetbrains.com) and [here](https://confluence.jetbrains.com/display/PLREP/features-extractor+integration+with+the+plugins.jetbrains.com).

5) [ide-diff-builder](ide-diff-builder) - module that evaluates API difference between IDE releases and builds external annotations `@ApiStatus.AvailableSince` and `@ApiStatus.ScheduledForRemoval`.

####Dependencies between the projects

Currently, the dependencies between the above projects are:

- **ide-diff-builder** - independent module
- **intellij-plugin-structure** - independent module
- **intellij-feature-extractor** - depends on the **intellij-plugin-structure**
- **intellij-plugin-verifier** - depends on the **intellij-plugin-structure**
- **plugins-verifier-service** - depends on the **intellij-feature-extractor** and **intellij-plugin-verifier**

####Configuring the local environment

1) Install the **intellij-plugin-structure** module locally by running 'gladle publishToMavenLocal' or 
a 'Install intellij-plugin-structure locally' run configuration.

2) Install the **intellij-feature-extractor** module locally by running the 'gladle publishToMavenLocal' or running 
a 'Install intellij-feature-extractor locally' run configuration.

3) Install the **intellij-plugin-verifier** module locally by running the 'gladle publishToMavenLocal' or running 
a 'Install intellij-plugin-verifier locally' run configuration.

4) If you face compilation errors in the plugins-verifier-service module, run the 'gradle war' or
a 'Build war plugins-verifier-service' run configuration.  