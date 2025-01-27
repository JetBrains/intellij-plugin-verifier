package com.jetbrains.pluginverifier.output.stream

import com.jetbrains.pluginverifier.output.BaseOutputPrintTest
import org.junit.Before
import org.junit.Test
import java.io.PrintWriter

class StreamOutputPrintTest : BaseOutputPrintTest<WriterResultPrinter>() {
  @Before
  override fun setUp() {
    super.setUp()
    resultPrinter = WriterResultPrinter(PrintWriter(out))
  }

  @Test
  fun `plugin is compatible`() {
    `when plugin is compatible` {
      val expected = """
      Plugin pluginId 1.0 against 232.0: Compatible

      
      """.trimIndent()
      assertOutput(expected)
    }
  }

  @Test
  fun `plugin has compatibility warnings`() {
    `when plugin has compatibility warnings` {
      val expected = """
        Plugin pluginId 1.0 against 232.0: 4 compatibility problems
        Compatibility problems (4): 
            #Incompatible change of super interface com.jetbrains.plugin.Parent to class
                Class com.jetbrains.plugin.Child has a *super interface* com.jetbrains.plugin.Parent which is actually a *class*. This can lead to **IncompatibleClassChangeError** exception at runtime.
            #Incompatible change of super interface com.jetbrains.plugin.pkg.Parent to class
                Class com.jetbrains.plugin.pkg.Child has a *super interface* com.jetbrains.plugin.pkg.Parent which is actually a *class*. This can lead to **IncompatibleClassChangeError** exception at runtime.
            #Invocation of unresolved method org.some.deleted.Class.foo() : void
                Method SomeClassUsingDeletedClass.someMethodReferencingDeletedClass() : void contains an *invokevirtual* instruction referencing an unresolved method org.some.deleted.Class.foo() : void. This can lead to **NoSuchMethodError** exception at runtime.
                Method SampleStuffFactory.produceStuff() : void contains an *invokevirtual* instruction referencing an unresolved method org.some.deleted.Class.foo() : void. This can lead to **NoSuchMethodError** exception at runtime.

     
      """.trimIndent()
      assertOutput(expected)
    }
  }

  @Test
  fun `plugin has structural problems`() {
    `when plugin has structural problems` {
      val expected = """
          Plugin pluginId 1.0 against 232.0: Compatible. 1 plugin configuration defect
          Plugin structure warnings (1): 
              Invalid plugin descriptor 'plugin.xml'. The plugin configuration file does not include any module dependency tags. So, the plugin is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA. Please note that plugins should declare a dependency on `com.intellij.modules.platform` to indicate dependence on shared functionality.

     
      """.trimIndent()
      assertOutput(expected)
    }
  }

  @Test
  fun `plugin has internal API usage problems`() {
    `when plugin has internal API usage problems` {
      val expected = """
          Plugin pluginId 1.0 against 232.0: Compatible. 1 usage of internal API
          Internal API usages (1): 
              #Internal class InternalApiRegistrar reference
                  Internal class InternalApiRegistrar is referenced in SampleStuffFactory.produceStuff() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.

     
      """.trimIndent()
      assertOutput(expected)
    }
  }

  @Test
  fun `plugin has non-extendable API usages problems`() {
    `when plugin has non-extendable API usages problems` {
      val expected = """
        Plugin pluginId 1.0 against 232.0: Compatible. 1 non-extendable API usage violation
        Non-extendable API usages (1): 
            #Non-extendable class NonExtendableClass is extended
                Non-extendable class NonExtendableClass is extended by ExtendingClass. This class is marked with @org.jetbrains.annotations.ApiStatus.NonExtendable, which indicates that the class is not supposed to be extended. See documentation of the @ApiStatus.NonExtendable for more info.

     
      """.trimIndent()
      assertOutput(expected)
    }
  }

  @Test
  fun `plugin has experimental API usage problems`() {
    `when plugin has experimental API usage problems` {
      val expected = """
        Plugin pluginId 1.0 against 232.0: Compatible. 1 usage of experimental API
        Experimental API usages (1): 
            #Experimental API class ExperimentalClass reference
                Experimental API class ExperimentalClass is referenced in ExtendingClass.someMethod() : void. This class can be changed in a future release leading to incompatibilities

     
      """.trimIndent()
      assertOutput(expected)
    }
  }

  @Test
  fun `plugin has missing dependencies`() {
    `when plugin has missing dependencies` {
      val expected = """
          Plugin pluginId 1.0 against 232.0: Compatible
          Missing dependencies: 
              MissingPlugin (optional): Dependency MissingPlugin is not found among the bundled plugins of IU-211.500

     
      """.trimIndent()
      assertOutput(expected)
    }
  }

  @Test
  fun `plugin is dynamic`() {
    `when plugin is dynamic` {
      val expected = """
          Plugin pluginId 1.0 against 232.0: Compatible
          Dynamic Plugin Eligibility:
              Plugin can probably be enabled or disabled without IDE restart

     
      """.trimIndent()
      assertOutput(expected)
    }
  }

  @Test
  fun `plugin has structural problems with invalid plugin ID`() {
    `when plugin has structural problems with invalid plugin ID` { resultPrinter, result ->
      resultPrinter.printInvalidPluginFiles(result)

      val expected = """
        The following files specified for the verification are not valid plugins:
            plugin.zip
                Additional plugin warnings:
                    Invalid plugin descriptor 'plugin.xml'. The plugin ID 'com.example.intellij' has a prefix 'com.example' that is not allowed.
                        This plugin problem has been reported since 2024-03-26. If the plugin was previously uploaded to the JetBrains Marketplace, it can be suppressed using the `-mute ForbiddenPluginIdPrefix` command-line switch.

      """.trimIndent()
      assertOutput(expected)
    }
  }

  @Test
  fun `plugin is dynamic and has structural warnings`() {
    `when plugin is dynamic and has structural warnings` {
      val expected = """
        Plugin pluginId 1.0 against 232.0: Compatible. 1 plugin configuration defect
        Plugin structure warnings (1): 
            Invalid plugin descriptor 'plugin.xml'. The plugin configuration file does not include any module dependency tags. So, the plugin is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA. Please note that plugins should declare a dependency on `com.intellij.modules.platform` to indicate dependence on shared functionality.
        Dynamic Plugin Eligibility:
            Plugin can probably be enabled or disabled without IDE restart


      """.trimIndent()
      assertOutput(expected)
    }
  }
}