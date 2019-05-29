package mock.plugin;

/*expected(PROBLEM)
 Plugin is marked as incompatible with IU-145.500

 Plugin org.some.company.plugin:1.0 is marked as incompatible with IU-145.500 in the special file 'brokenPlugins.txt' bundled to the IDE distribution. This option is used to prevent loading of broken plugins, which may lead to IDE startup errors, if the plugins remain locally installed (in config>/plugins directory) and the IDE is updated to newer version where this plugin is no more compatible. The new IDE will refuse to load this plugin with a message 'The following plugins are incompatible with the current IDE build: org.some.company.plugin' or similar.
*/
public class PluginIsMarkedIncompatible {
}
