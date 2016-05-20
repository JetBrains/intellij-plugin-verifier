package com.intellij.structure.impl.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Ref<T> {
  private T myValue;

  public Ref() {
  }

  public Ref(@Nullable T value) {
    myValue = value;
  }

  @NotNull
  public static <T> Ref<T> create() {
    return new Ref<T>();
  }

  public static <T> Ref<T> create(@Nullable T value) {
    return new Ref<T>(value);
  }

  @Nullable
  public static <T> T deref(@Nullable Ref<T> ref) {
    return ref == null ? null : ref.get();
  }

  public final boolean isNull() {
    return myValue == null;
  }

  public final T get() {
    return myValue;
  }

  public final void set(@Nullable T value) {
    myValue = value;
  }

  public final boolean setIfNull(@Nullable T value) {
    if (myValue == null) {
      myValue = value;
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return String.valueOf(myValue);
  }
}
