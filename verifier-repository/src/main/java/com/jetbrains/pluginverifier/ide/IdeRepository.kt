package com.jetbrains.pluginverifier.ide

import com.google.gson.annotations.SerializedName
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

/**
 * Provides index of IDE builds available for downloading.
 * The index is fetched from the data service https://data.services.jetbrains.com/products.
 */
class IdeRepository(private val dataServicesUrl: String = DEFAULT_DATA_SERVICES_URL) {

  companion object {
    private const val DEFAULT_DATA_SERVICES_URL = "https://data.services.jetbrains.com"
  }

  private val dataServiceConnector by lazy {
    Retrofit.Builder()
        .baseUrl(dataServicesUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(ProductsConnector::class.java)
  }

  /**
   * Fetches available IDEs index from the data service.
   */
  fun fetchIndex(): List<AvailableIde> {
    val products = dataServiceConnector.getProducts().executeSuccessfully().body()
    return DataSourceIndexParser().parseAvailableIdes(products)
  }

  /**
   * Returns [AvailableIde] for this [ideVersion] if it is still available.
   */
  fun fetchAvailableIde(ideVersion: IdeVersion): AvailableIde? {
    val fullIdeVersion = ideVersion.setProductCodeIfAbsent("IU")
    return fetchIndex().find { it.version == fullIdeVersion }
  }

}

fun IdeVersion.setProductCodeIfAbsent(productCode: String) =
    if (this.productCode.isEmpty())
      IdeVersion.createIdeVersion("$productCode-" + asStringWithoutProductCode())
    else {
      this
    }


internal interface ProductsConnector {
  @GET("products")
  fun getProducts(): Call<List<Product>>
}

internal data class Product(
    @SerializedName("code")
    val code: String,

    @SerializedName("alternativeCodes")
    val alternativeCodes: List<String>,

    @SerializedName("name")
    val name: String,

    @SerializedName("productFamilyName")
    val productFamilyName: String,

    @SerializedName("link")
    val link: String,

    @SerializedName("releases")
    val releases: List<Release>
)

internal data class Release(
    @SerializedName("date")
    val date: String,

    @SerializedName("version")
    val version: String?,

    @SerializedName("majorVersion")
    val majorVersion: String,

    @SerializedName("build")
    val build: String?,

    @SerializedName("type")
    val type: String,

    @SerializedName("notesLink")
    val notesLink: String,

    @SerializedName("printableReleaseType")
    val printableReleaseType: String?,

    @SerializedName("downloads")
    val downloads: Map<String, Download>?
)

internal data class Download(
    @SerializedName("link")
    val link: String,

    @SerializedName("size")
    val size: Long
)
