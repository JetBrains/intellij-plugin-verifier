### IntelliJ Feature Extractor
This is a tool used by [JetBrains Marketplace](https://plugins.jetbrains.com/) to determine a set of supported plugin's features.

The features are specified using IntelliJ API. In most cases the features are specified as string constants. The tool extracts the constant values from class files
using bytecode analysis.
There are tricky cases which are hard to analyse without running the plugin's code. In such cases the feature extractor
may return incomplete results.
The list of extracted plugin features can be seen on this [page](https://plugins.jetbrains.com/feature).

### Configuration types 
A plugin author implements interface <code>com.intellij.execution.configurations.ConfigurationType</code> and specifies the <code>id</code> of the configuration type as return value of <code>getId()</code>
### Facet types
A plugin author implements abstract class <code>com.intellij.facet.FacetType</code> as passes the facet type <code>id</code> as a second parameter to one of <code>FacetType</code>'s constructor.
### File extensions
A plugin author implements abstract class <code>com.intellij.openapi.fileTypes.FileTypeFactory</code> and feeds the <code>com.intellij.openapi.fileTypes.FileTypeConsumer</code> to the only abstract method with supported file extensions.
### Artifact types
A plugin author implements abstract class <code>com.intellij.packaging.artifacts.ArtifactType</code> and passes the artifact <code>id</code> as first parameter to <code>ArtifactType</code>'s constructor.
### Module type
A plugin author implements abstract class <code>com.intellij.openapi.module.ModuleType</code>
and passes the module <code>id</code> to super class constructor.
### Dependency support
A plugin author declares a tag like `<dependencySupport kind="java" coordinate="org.junit:org.junit" displayName="Junit"/>` in the `plugin.xml`
