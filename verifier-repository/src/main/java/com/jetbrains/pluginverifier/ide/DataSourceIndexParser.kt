package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.net.URL

/**
 * Parser of the data-services index - https://data.services.jetbrains.com/products.
 *
 * [Parses] [parseAvailableIdes] list of [Product]s to list of [AvailableIde]s.
 *
 * It recognises only products listed in [productCodeMapping] and ignores the rest.
 */
class DataSourceIndexParser {
  /**
   * Maps data-service's product codes to common product codes.
   * For some products they are the same.
   */
  private val productCodeMapping = mapOf(
      "CL" to "CL",   //CLion
      "DG" to "DB",   //DataGrip
      "IIC" to "IC",  //IDEA Community
      "IIU" to "IU",  //IDEA Ultimate
      "PCC" to "PC",  //PyCharm Community
      "PCP" to "PY",  //PyCharm Ultimate
      "GO" to "GO",   //GoLand
      "PS" to "PS",   //PhpStorm
      "RD" to "RD",   //Rider
      "RM" to "RM",   //RubyMine
      "WS" to "WS",   //WebStorm
      "MPS" to "MPS"  //MPS
  )

  internal fun parseAvailableIdes(products: List<Product>): List<AvailableIde> {
    val availableIdes = arrayListOf<AvailableIde>()
    for (product in products) {
      val productCode = getProductCode(product)
      if (productCode != null) {
        for (release in product.releases) {
          if (release.downloads != null) {
            val download = getBuildDownload(release.downloads)
            if (download != null && release.build != null) {
              val downloadUrl = URL(download.link)
              val ideVersion = IdeVersion.createIdeVersionIfValid(release.build)
                  ?.setProductCodeIfAbsent(productCode)
              if (ideVersion != null) {
                val releaseVersion = getReleaseVersion(release)
                val availableIde = AvailableIde(ideVersion, releaseVersion, downloadUrl)
                availableIdes.add(availableIde)
              }
            }
          }
        }
      }
    }
    return availableIdes
  }

  /**
   * Release version is only applicable for "release" IDEs.
   */
  private fun getReleaseVersion(release: Release) =
      if (release.type == "release") release.version else null

  private fun getProductCode(product: Product) =
      productCodeMapping[product.code]

  /**
   * "downloads" map provides download URL for different distribution types.
   *
   * All IntelliJ IDEs have either "linux" (IU, IC, PC...) or "zip" (MPS) distribution.
   */
  private fun getBuildDownload(downloads: Map<String, Download>): Download? =
      downloads["linux"] ?: downloads["zip"]
}