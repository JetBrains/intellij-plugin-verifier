### IntelliJ Plugin Structure
Library used to parse configuration of JetBrains Plugins and validate its correctness.
It is used by [gradle-intellij-plugin](https://github.com/JetBrains/gradle-intellij-plugin), [JetBrains Marketplace](https://plugins.jetbrains.com/)
and the [IntelliJ Plugin Verifier](https://github.com/JetBrains/intellij-plugin-verifier).

The library detects configuration mistakes and produces corresponding warnings like `Description is too short` or 
even errors like `Directory 'lib' must not be empty` according to verification [rules](https://youtrack.jetbrains.com/issue/MP-420).

There are different types of plugins: 

| Plugin Type | Module               | Manager -> .createPlugin(Path pluginFile) | API               | 
|-------------|----------------------|-------------------------------------------|-------------------|  
| IntelliJ    | `structure-intellij` | `IdePluginManager.createManager()`        | `IdePlugin`       |
| ReSharper   | `structure-dotnet`   | `ReSharperPluginManager.createManager()`  | `ReSharperPlugin` |
| EDU         | `structure-edu`      | `EduPluginManager.createManager()`        | `EduPlugin`       |
| Fleet       | `structure-fleet`    | `FleetPluginManager.createManager()`      | `FleetPlugin`     |
| Hub         | `structure-hub`      | `HubPluginManager.createManager()`        | `HubPlugin`       |
| TeamCity    | `structure-teamcity` | `TeamcityPluginManager.createManager()`   | `TeamcityPlugin`  |
| YouTrack    | `structure-youtrack` | `YouTrackPluginManager.createManager()`   | `YouTrackPlugin`  |

*IntelliJ Plugins* may be in several forms:
- single `.jar` file containing `/META-INF/plugin.xml`
- `.zip` archive containing the single `PluginName` directory containing `lib/*.jar` files one of which contains `/META-INF/plugin.xml`
- extracted `.zip` archive of the above form

#### Additional modules
`structure-classes` module contains APIs for resolving plugin class files, which is necessary for the [IntelliJ Plugin Verifier](https://github.com/JetBrains/intellij-plugin-verifier) to verify classes bytecode.

`structure-ide` and `structure-ide-classes` modules contain APIs for parsing configurations of IntelliJ IDE builds along with their bundled plugins, which is necessary for the IntelliJ Plugin Verifier as well.
