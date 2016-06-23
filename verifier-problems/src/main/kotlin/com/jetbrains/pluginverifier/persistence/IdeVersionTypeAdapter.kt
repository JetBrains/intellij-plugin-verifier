package com.jetbrains.pluginverifier.persistence

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.intellij.structure.domain.IdeVersion

import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
class IdeVersionTypeAdapter : TypeAdapter<IdeVersion>() {

  @Throws(IOException::class)
  override fun write(out: JsonWriter, value: IdeVersion) {
    out.value(value.asString())
  }

  @Throws(IOException::class)
  override fun read(`in`: JsonReader): IdeVersion {
    return IdeVersion.createIdeVersion(`in`.nextString())
  }

}
