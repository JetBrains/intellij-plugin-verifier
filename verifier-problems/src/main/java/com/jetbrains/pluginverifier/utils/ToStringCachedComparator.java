package com.jetbrains.pluginverifier.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.IdentityHashMap;

public class ToStringCachedComparator<T> implements Comparator<T> {

  private final IdentityHashMap<T, String> myCache = new IdentityHashMap<T, String>();

  @NotNull
  protected String toString(T object) {
    return object.toString();
  }

  @NotNull
  private String getDescriptor(T obj) {
    String res = myCache.get(obj);
    if (res == null) {
      res = toString(obj);
      myCache.put(obj, res);
    }

    return res;
  }

  @Override
  public int compare(T o1, T o2) {
    return getDescriptor(o1).compareTo(getDescriptor(o2));
  }
}
