package com.jetbrains.pluginverifier.persistence;

import com.google.common.base.Preconditions;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>
 * The class allows to serialize {@link Jsonable}s.
 * <p>
 * The serialized form looks as follows:<p>
 * {@code {"class": "class_name", "content": {"param_1": "value_1", ... "param_n": "value_n"}}}
 *
 * @author Sergey Patrikeev
 */
public class JsonableTypeAdapter<T> extends TypeAdapter<Jsonable<T>> {

  private static final String CLASS_FIELD = "class";
  private static final String CONTENT_FIELD = "content";
  private ConcurrentMap<String, Jsonable<?>> myFactories = new ConcurrentHashMap<String, Jsonable<?>>();

  @Override
  public void write(JsonWriter out, Jsonable<T> value) throws IOException {
    out.beginObject();
    try {
      out.name(CLASS_FIELD);
      out.value(value.getClass().getName());
      out.name(CONTENT_FIELD);
      out.beginObject();
      try {
        for (Pair<String, String> param : value.serialize()) {
          out.name(param.getFirst());
          out.value(param.getSecond());
        }
      } finally {
        out.endObject();
      }
    } finally {
      out.endObject();
    }
  }

  @Override
  public Jsonable<T> read(JsonReader in) throws IOException {
    in.beginObject();
    try {
      Preconditions.checkArgument(CLASS_FIELD.equals(in.nextName()), "invalid JSON argument");
      String clsName = in.nextString();
      Jsonable<?> factory = getFactory(clsName);

      Preconditions.checkArgument(CONTENT_FIELD.equals(in.nextName()), "invalid JSON argument");

      in.beginObject();
      try {
        List<String> params = new ArrayList<String>(); //it is not optimal for expected 1-3 parameters, but it produces cleaner code

        while (in.hasNext()) {
          in.nextName(); //skip parameter name
          if (in.peek() == JsonToken.NULL) {
            params.add(null);
            in.nextNull();
          } else {
            params.add(in.nextString());
          }
        }

        return (Jsonable<T>) factory.deserialize(params.toArray(new String[0]));
      } finally {
        in.endObject();
      }
    } finally {
      in.endObject();
    }
  }

  @NotNull
  private Jsonable<?> getFactory(@NotNull String clsName) {
    Jsonable<?> result = myFactories.get(clsName);
    if (result == null) {
      //we are not worried about multiple initiations of factory instances.
      try {
        Class<?> aClass = Class.forName(clsName);
        if (!Jsonable.class.isAssignableFrom(aClass)) {
          throw new IllegalArgumentException("Supplied class is not an instance of com.jetbrains.pluginverifier.persistence.Jsonable class");
        }
        result = ((Jsonable<?>) aClass.newInstance());
        myFactories.put(clsName, result);
      } catch (Exception e) {
        throw new IllegalArgumentException("Unable to initialize a Jsonable class factory " + clsName + ". Check that the Jsonable class contains a public default constructor", e);
      }
    }
    return result;
  }


}
