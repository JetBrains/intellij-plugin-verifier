package com.intellij.util.indexing;

import org.jetbrains.annotations.NotNull;

public abstract class IndexExtension<Key, Value, Input> {
  public abstract @NotNull IndexId<Key, Value> getName();
  @SuppressWarnings("unused")
  public abstract @NotNull DataIndexer<Key, Value, Input> getIndexer();
}