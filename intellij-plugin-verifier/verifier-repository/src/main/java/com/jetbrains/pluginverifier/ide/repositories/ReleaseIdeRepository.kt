/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.ide.repositories

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.ide.DataServicesIndexParser
import com.jetbrains.pluginverifier.misc.RestApiFailed
import com.jetbrains.pluginverifier.misc.RestApiOk
import com.jetbrains.pluginverifier.misc.RestApis
import com.jetbrains.pluginverifier.repository.cache.memoize

private const val DATA_SERVICES_URL = "https://data.services.jetbrains.com"

/**
 * Provides index of IDE builds available for downloading.
 * The index is fetched from the data service https://data.services.jetbrains.com/products.
 */
class ReleaseIdeRepository(private val dataServicesUrl: String = DATA_SERVICES_URL) : IdeRepository {
  private val dataServicesConnector by lazy {
    DataServicesConnector(dataServicesUrl)
  }

  private val indexCache = memoize { updateIndex() }

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
  @JsonProperty("code")
  val code: String,

  @JsonProperty("alternativeCodes")
  val alternativeCodes: List<String>,

  @JsonProperty("name")
  val name: String,

  @JsonProperty("productFamilyName")
  val productFamilyName: String,

  @JsonProperty("link")
  val link: String,

  @JsonProperty("releases")
  val releases: List<Release>
)

internal data class Release(
  @JsonProperty("date")
  val date: String,

  @JsonProperty("version")
  val version: String?,

  @JsonProperty("majorVersion")
  val majorVersion: String,

  @JsonProperty("build")
  val build: String?,

  @JsonProperty("type")
  val type: String,

  @JsonProperty("notesLink")
  val notesLink: String?,

  @JsonProperty("printableReleaseType")
  val printableReleaseType: String?,

  @JsonProperty("downloads")
  val downloads: Map<String, Download>?
)

internal data class Download(
  @JsonProperty("link")
  val link: String,

  @JsonProperty("size")
  val size: Long
)