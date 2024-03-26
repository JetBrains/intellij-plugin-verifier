package com.jetbrains.plugin.structure.intellij.problems.ignored

import com.fasterxml.jackson.core.exc.StreamReadException
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.plugin.structure.intellij.problems.LevelRemappingPluginCreationResultResolver
import java.io.IOException
import java.net.URL

private const val CLI_IGNORED_PROBLEMS_FILE_NAME = "plugin-problems-cli-muteable.json"

class CliIgnoredProblemDefinitionLoader(private val jsonUrl: URL) {
  constructor() : this(
    LevelRemappingPluginCreationResultResolver::class.java.getResource(CLI_IGNORED_PROBLEMS_FILE_NAME)
      ?: throw IOException("Definition for problems that can be ignored in the CLI switch " +
        "cannot be found at <$CLI_IGNORED_PROBLEMS_FILE_NAME>")
  )

  private val json = ObjectMapper()

  @Throws(IOException::class)
  fun load(): List<CliIgnoredPluginProblem> {
    return try {
      val rawDefinitions: List<Map<String, String>> = json.readValue(jsonUrl)
      rawDefinitions.map {
        CliIgnoredPluginProblem(
          it["id"] ?: throw IOException("Missing 'id' field in the definition of a CLI ignored problem"),
          it["class"]
            ?: throw IOException("Missing 'class' field in the definition of a CLI ignored problem"),
          it["since"] ?: throw IOException("Missing 'since' field in the definition of a CLI ignored problem")
        )
      }
    } catch (e: IOException) {
      throw IOException("Cannot load CLI ignorable problems definitions from <$jsonUrl>", e)
    } catch (e: StreamReadException) {
      throw IOException("Cannot parse CLI ignorable problems definitions from JSON in <$jsonUrl>", e)
    } catch (e: DatabindException) {
      throw IOException("Cannot deserialize CLI ignorable problems definitions from JSON in <$jsonUrl>", e)
    }
  }
}
