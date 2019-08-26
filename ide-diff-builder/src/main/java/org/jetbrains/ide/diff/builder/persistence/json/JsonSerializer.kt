package org.jetbrains.ide.diff.builder.persistence.json

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val jsonInstance: Json = Json(
    JsonConfiguration.Stable.copy(
        strictMode = false,
        prettyPrint = true,
        indent = "  "
    )
)

@Serializer(forClass = IdeVersion::class)
object IdeVersionSerializer {
  override val descriptor
    get() = StringDescriptor

  override fun serialize(encoder: Encoder, obj: IdeVersion) {
    encoder.encodeString(obj.asString())
  }

  override fun deserialize(decoder: Decoder) =
      IdeVersion.createIdeVersion(decoder.decodeString())
}