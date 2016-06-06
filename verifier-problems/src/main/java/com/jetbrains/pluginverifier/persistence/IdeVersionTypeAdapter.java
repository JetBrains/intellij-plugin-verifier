package com.jetbrains.pluginverifier.persistence;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.intellij.structure.domain.IdeVersion;

import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
class IdeVersionTypeAdapter extends TypeAdapter<IdeVersion> {

  @Override
  public void write(JsonWriter out, IdeVersion value) throws IOException {
    out.value(value.asString());
  }

  @Override
  public IdeVersion read(JsonReader in) throws IOException {
    return IdeVersion.createIdeVersion(in.nextString());
  }

}
