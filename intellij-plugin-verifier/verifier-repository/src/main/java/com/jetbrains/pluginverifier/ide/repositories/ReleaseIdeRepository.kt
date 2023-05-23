/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.ide.repositories

import com.google.common.base.Suppliers
import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.ide.DataServicesIndexParser
import com.jetbrains.pluginverifier.misc.RestApiFailed
import com.jetbrains.pluginverifier.misc.RestApiOk
import com.jetbrains.pluginverifier.misc.RestApis
import java.util.concurrent.TimeUnit

private const val DATA_SERVICES_URL = "https://data.services.jetbrains.com"

/**
 * Provides index of IDE builds available for downloading.
 * The index is fetched from the data service https://data.services.jetbrains.com/products.
 */
class ReleaseIdeRepository(private val dataServicesUrl: String = DATA_SERVICES_URL) : IdeRepository {
  private val dataServicesConnector by lazy {
    DataServicesConnector(dataServicesUrl)
  }

  private val indexCache = Suppliers.memoizeWithExpiration<List<AvailableIde>>(this::updateIndex, 5, TimeUnit.MINUTES)

  private fun updateIndex(): List<AvailableIde> {
    val products = dataServicesConnector.getProducts()
    return DataServicesIndexParser().parseAvailableIdes(products)
  }

  @Throws(InterruptedException::class)
  override fun fetchIndex(): List<AvailableIde> = indexCache.get()

  override fun toString() = "IDE repository based on $DATA_SERVICES_URL/products"
}

private class DataServicesConnector(private val dataServicesUrl: String) {
  private val restApi = RestApis()

  fun getProducts(): List<Product> {
    val uri = "$dataServicesUrl/products"
    return when (val apiResult = restApi.getList(uri, Product::class.java)) {
      is RestApiOk<List<Product>> -> apiResult.payload
      is RestApiFailed<*> -> emptyList()
    }
  }
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