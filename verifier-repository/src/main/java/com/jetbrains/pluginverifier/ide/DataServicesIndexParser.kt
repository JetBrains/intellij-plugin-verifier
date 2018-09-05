package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.ide.IntelliJPlatformProduct
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.net.URL

/**
 * Parser of the data-services index - https://data.services.jetbrains.com/products.
 *
 * [Parses] [parseAvailableIdes] list of [Product]s to list of [AvailableIde]s.
 *
 * It recognises only products listed in [productCodeMapping] and ignores the rest.
 */
internal class DataServicesIndexParser {
  internal fun parseAvailableIdes(products: List<Product>): List<AvailableIde> {
    val availableIdes = arrayListOf<AvailableIde>()
    for (product in products) {
      val intelliJPlatformProduct = getIntelliJProduct(product)
      if (intelliJPlatformProduct != null) {
        for (release in product.releases) {
          if (release.downloads != null) {
            val download = getBuildDownload(release.downloads)
            if (download != null && release.build != null) {
              val downloadUrl = URL(download.link)
              val ideVersion = IdeVersion.createIdeVersionIfValid(release.build)
                  ?.setProductCodeIfAbsent(intelliJPlatformProduct.productCode)
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

  private fun getIntelliJProduct(product: Product) =
      IntelliJPlatformProduct.fromProductCode(product.code)

  /**
   * "downloads" map provides download URL for different distribution types.
   *
   * All IntelliJ IDEs have either "linux" (IU, IC, PC...) or "zip" (MPS) distribution.
   */
  private fun getBuildDownload(downloads: Map<String, Download>): Download? =
      downloads["linux"] ?: downloads["zip"]
}