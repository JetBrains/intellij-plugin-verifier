package com.jetbrains.pluginverifier.ide

import com.google.common.base.Suppliers
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
class ReleaseIdeRepository : IdeRepository {

  companion object {
    private const val DATA_SERVICES_URL = "https://data.services.jetbrains.com"
  }

  private val dataServiceConnector by lazy {
    Retrofit.Builder()
        .baseUrl(DATA_SERVICES_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(ProductsConnector::class.java)
  }

  private val indexCache = Suppliers.memoizeWithExpiration<List<AvailableIde>>(this::updateIndex, 1, TimeUnit.MINUTES)

  private fun updateIndex(): List<AvailableIde> {
    val products = dataServiceConnector.getProducts().executeSuccessfully().body()
    return DataServicesIndexParser().parseAvailableIdes(products)
  }


  @Throws(InterruptedException::class)
  override fun fetchIndex(): List<AvailableIde> = indexCache.get()

  @Throws(InterruptedException::class)
  override fun fetchAvailableIde(ideVersion: IdeVersion): AvailableIde? {
    val fullIdeVersion = ideVersion.setProductCodeIfAbsent("IU")
    return fetchIndex().find { it.version == fullIdeVersion }
  }

  override fun toString() = "IDE repository based on $DATA_SERVICES_URL/products"

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

fun IdeVersion.setProductCodeIfAbsent(productCode: String) =
    if (this.productCode.isEmpty())
      IdeVersion.createIdeVersion("$productCode-" + asStringWithoutProductCode())
    else {
      this
    }