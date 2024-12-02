/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

open class BaseEventFilterTest {

  protected fun captureToString(capturer: ByteArrayOutputStream.() -> Unit): String {
    return ByteArrayOutputStream().use {
      capturer(it)
      it.toString(Charsets.UTF_8)
    }
  }

  protected fun resourceStream(name: String): InputStream {
    val resourceAsStream: InputStream? = PluginXmlDependencyFilterTest::class.java.getResourceAsStream(name)
    checkNotNull(resourceAsStream)
    return resourceAsStream
  }

  protected fun String.toInputStream() = ByteArrayInputStream(this.toByteArray())
}