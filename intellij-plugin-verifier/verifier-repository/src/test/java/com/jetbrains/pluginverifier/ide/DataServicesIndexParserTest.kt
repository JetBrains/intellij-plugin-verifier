package com.jetbrains.pluginverifier.ide

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.jetbrains.plugin.structure.intellij.version.IdeVersion.createIdeVersion
import com.jetbrains.pluginverifier.ide.repositories.Product
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL
import java.time.LocalDate

/**
 * Ensures that the data service's index is parsed properly:
 * `https://data.services.jetbrains.com/products`
 */
class DataServicesIndexParserTest {

  @Test
  fun `simple index parsing test`() {
    val products = Gson().fromJson<List<Product>>(
        DataServicesIndexParserTest::class.java.getResourceAsStream("/releaseIdeRepositoryIndex.json").bufferedReader()
    )

    val actualIdes = DataServicesIndexParser().parseAvailableIdes(products)
    val expectedIdes = listOf(
        availableIde("CL-182.3458.13", null, "/cpp/CLion-182.3458.13.tar.gz", LocalDate.of(2018, 6, 27)),
        availableIde("CL-181.5281.33", "2018.1.5", "/cpp/CLion-2018.1.5.tar.gz", LocalDate.of(2018, 6, 15)),
        availableIde("IC-182.3458.5", null, "/idea/ideaIC-182.3458.5.tar.gz", LocalDate.of(2018, 6, 26)),
        availableIde("IC-181.5281.24", "2018.1.5", "/idea/ideaIC-2018.1.5.tar.gz", LocalDate.of(2018, 6, 13)),
        availableIde("IU-182.3458.5", null, "/idea/ideaIU-182.3458.5.tar.gz", LocalDate.of(2018, 6, 26)),
        availableIde("IU-181.5281.24", "2018.1.5", "/idea/ideaIU-2018.1.5.tar.gz", LocalDate.of(2018, 6, 13)),
        availableIde("MPS-181.1469", "2018.1.5", "/mps/2018.1/MPS-2018.1.5.tar.gz", LocalDate.of(2018, 6, 15)),
        availableIde("MPS-181.1404", "2018.1.4", "/mps/2018.1/MPS-2018.1.4.tar.gz", LocalDate.of(2018, 5, 24)),
        availableIde("PC-182.3458.8", null, "/python/pycharm-community-182.3458.8.tar.gz", LocalDate.of(2018, 6, 28)),
        availableIde("PC-182.3341.8", null, "/python/pycharm-community-182.3341.8.tar.gz", LocalDate.of(2018, 6, 20)),
        availableIde("PC-181.5087.37", "2018.1.4", "/python/pycharm-community-2018.1.4.tar.gz", LocalDate.of(2018, 5, 31)),
        availableIde("PY-182.3458.8", null, "/python/pycharm-professional-182.3458.8.tar.gz", LocalDate.of(2018, 6, 28)),
        availableIde("PY-181.5087.37", "2018.1.4", "/python/pycharm-professional-2018.1.4.tar.gz", LocalDate.of(2018, 5, 31)),
        availableIde("PS-182.3458.35", null, "/webide/PhpStorm-182.3458.35.tar.gz", LocalDate.of(2018, 6, 27)),
        availableIde("PS-182.3341.34", null, "/webide/PhpStorm-182.3341.34.tar.gz", LocalDate.of(2018, 6, 20)),
        availableIde("PS-181.5281.35", "2018.1.6", "/webide/PhpStorm-2018.1.6.tar.gz", LocalDate.of(2018, 6, 15))
    )

    assertEquals(expectedIdes, actualIdes)
  }

  private fun availableIde(version: String, releaseVersion: String?, url: String, uploadDate: LocalDate) =
      AvailableIde(createIdeVersion(version), releaseVersion, URL(URL("https://download.jetbrains.com"), url), uploadDate)

}