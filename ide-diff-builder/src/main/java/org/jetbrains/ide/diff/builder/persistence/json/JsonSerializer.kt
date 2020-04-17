/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.persistence.json

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val jsonInstance: Json = Json(
  JsonConfiguration.Stable.copy(
    isLenient = true,
    ignoreUnknownKeys = true,
    prettyPrint = true,
    indent = "  "
  )
)

@Serializer(forClass = IdeVersion::class)
object IdeVersionSerializer {
  override val descriptor
    get() = PrimitiveDescriptor("IdeVersionSerializer", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, obj: IdeVersion) {
    encoder.encodeString(obj.asString())
  }

  override fun deserialize(decoder: Decoder) =
    IdeVersion.createIdeVersion(decoder.decodeString())
}