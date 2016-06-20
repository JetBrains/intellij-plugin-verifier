package com.jetbrains.pluginverifier.persistence;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * The class allows to serialize all the {@code Multimap<K, V>} generified classes.
 *
 * @author Sergey Patrikeev
 */
public class MultimapTypeAdapterFactory implements TypeAdapterFactory {
  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
    Type type = typeToken.getType();
    if (!Multimap.class.isAssignableFrom(typeToken.getRawType()) || !(type instanceof ParameterizedType)) {
      return null;
    }
    Type[] types = ((ParameterizedType) type).getActualTypeArguments();
    Type keyType = types[0];
    Type valueType = types[1];
    TypeAdapter<?> keyAdapter = gson.getAdapter(TypeToken.get(keyType));
    TypeAdapter<?> valueAdapter = gson.getAdapter(TypeToken.get(valueType));
//      noinspection unchecked (no problems)
    return (TypeAdapter<T>) newMultimapAdapter(keyAdapter, valueAdapter);

  }

  private <K, V> TypeAdapter<Multimap<K, V>> newMultimapAdapter(final TypeAdapter<K> keyAdapter, final TypeAdapter<V> valueAdapter) {
    return new TypeAdapter<Multimap<K, V>>() {
      @Override
      public void write(JsonWriter out, Multimap<K, V> value) throws IOException {
        out.beginArray();
        for (K k : value.keySet()) {
          keyAdapter.write(out, k);
          out.beginArray();
          for (V v : value.get(k)) {
            valueAdapter.write(out, v);
          }
          out.endArray();
        }
        out.endArray();
      }

      @Override
      public Multimap<K, V> read(JsonReader in) throws IOException {
        LinkedHashMultimap<K, V> result = LinkedHashMultimap.create();
        in.beginArray();
        while (in.hasNext()) {
          K k = keyAdapter.read(in);
          in.beginArray();
          while (in.hasNext()) {
            V v = valueAdapter.read(in);
            result.put(k, v);
          }
          in.endArray();
        }

        in.endArray();
        return result;
      }
    }.nullSafe(); //Gson will check nulls automatically
  }
}
