package com.jetbrains.plugin.structure.intellij.platform

import com.fasterxml.jackson.core.exc.StreamReadException
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.plugin.structure.base.utils.inputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.file.Path

internal class ProductInfoParser {
  private val jackson = ObjectMapper()

  @Throws(ProductInfoParseException::class)
  fun parse(productInfoJsonPath: Path): ProductInfo {
    return parse(productInfoJsonPath.inputStream(), productInfoJsonPath.toString())
  }

  @Throws(ProductInfoParseException::class)
  fun parse(productInfoJsonUrl: URL): ProductInfo =
    productInfoJsonUrl.openStream().use {
      parse(it, productInfoJsonUrl.toString())
    }

  @Throws(ProductInfoParseException::class)
  fun parse(inputStream: InputStream, streamLocation: String): ProductInfo {
    try {
      return jackson.readValue<ProductInfo>(inputStream)
    } catch (e: Exception) {
      when (e) {
        is StreamReadException,
        is DatabindException,
        is IOException  -> throw ProductInfoParseException(
          "Cannot load 'product-info.json' from [$streamLocation]", e)
        else -> throw e
      }
    }
  }
}

internal class ProductInfoParseException(message: String, cause: Exception) : Exception(message, cause)