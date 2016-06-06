package com.jetbrains.pluginverifier.persistence;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.jetbrains.pluginverifier.format.UpdateInfo;

import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
class UpdateInfoTypeAdapter extends TypeAdapter<UpdateInfo> {
  @Override
  public void write(JsonWriter out, UpdateInfo value) throws IOException {
    out.beginArray();
    out.value(value.getPluginId());
    out.value(value.getPluginName());
    out.value(value.getVersion());
    out.value(value.getUpdateId());
    //we don't serialize CDate because it's useless.
    out.endArray();
  }

  @Override
  public UpdateInfo read(JsonReader in) throws IOException {
    in.beginArray();
    String id = getStringOrNull(in);
    String name = getStringOrNull(in);
    String version = getStringOrNull(in);
    Integer updateId = null;

    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
    } else {
      updateId = in.nextInt();
    }

    in.endArray();
    return new UpdateInfo(updateId, id, name, version);
  }

  private String getStringOrNull(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    } else {
      return in.nextString();
    }
  }

}
