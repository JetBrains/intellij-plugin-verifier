package com.jetbrains.plugin.structure.teamcity.action

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

class ParseValidFullActionTest(
  fileSystemType: FileSystemType,
) : BasePluginManagerTest<TeamCityActionPlugin, TeamCityActionPluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path): TeamCityActionPluginManager =
    TeamCityActionPluginManager.createManager(extractDirectory)

  private val actionYaml =
    """
    ---
    spec-version: 1.0.0
    name: namespace/simple-action
    version: 1.2.3
    description: this is a simple action
    inputs:
      - some text input:
          type: text
          label: text input
          default: a default text value
          required: false
          description: a description for the text input
      - some select input:
          type: select
          label: select input
          options:
            - first select option
            - second select option
          default: first select option
          required: true
          description: description for select input
      - some boolean input:
          type: boolean
          default: false
    requirements:
      - requirement 0:
          type: exists
      - requirement 1:
          type: not-exists
      - requirement 2:
          type: equals
          value: some-value
      - requirement 3:
          type: not-equals
          value: some-value
      - requirement 4:
          type: more-than
          value: 1
      - requirement 5:
          type: not-more-than
          value: 1
      - requirement 6:
          type: less-than
          value: 1
      - requirement 7:
          type: not-less-than
          value: 1
      - requirement 8:
          type: starts-with
          value: some-value
      - requirement 9:
          type: contains
          value: some-value
      - requirement 10:
          type: does-not-match
          value: some-value
      - requirement 11:
          type: version-more-than
          value: some-value
      - requirement 12:
          type: version-not-more-than
          value: some-value
      - requirement 13:
          type: version-less-than
          value: some-value
      - requirement 14:
          type: version-not-less-than
          value: some-value
    steps:
      - name: step 1
        with: runner/maven
        params:
          pomLocation: pom.xml
          goals: build
          one more param: one more value
      - script: echo "step 2 output"
        name: step 2
      - name: step 3
        with: action/name@1.2.3
        params:
          text-input: passed text parameter value
          select-input: first select option
          boolean-input: true
    """.trimIndent()

  @Test
  fun `parse full valid TeamCity Action from YAML`() {
    // arrange
    val fileName = "action.yaml"
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file(fileName) { actionYaml }
    }

    // act
    val result = createPluginSuccessfully(pluginFile)

    // assert
    with(result.plugin) {
      assertEquals(fileName, this.yamlFile.fileName)
      assertArrayEquals(actionYaml.toByteArray(), this.yamlFile.content)
      assertEquals("1.0.0", this.specVersion)
      assertEquals("namespace/simple-action", this.pluginName)
      assertEquals("namespace", this.namespace)
      assertEquals("1.2.3", this.pluginVersion)
      assertEquals("this is a simple action", this.description)
    }
  }
}