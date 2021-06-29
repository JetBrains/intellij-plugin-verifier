/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.persistence.json

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

val jsonInstance: Json = Json {
  isLenient = true
  ignoreUnknownKeys = true
  prettyPrint = true
  prettyPrintIndent = "  "
}

@Serializer(forClass = IdeVersion::class)
object IdeVersionSerializer {
  override val descriptor
    get() = PrimitiveSerialDescriptor("IdeVersionSerializer", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: IdeVersion) {
    encoder.encodeString(value.asString())
  }

  override fun deserialize(decoder: Decoder) =
    IdeVersion.createIdeVersion(decoder.decodeString())
}