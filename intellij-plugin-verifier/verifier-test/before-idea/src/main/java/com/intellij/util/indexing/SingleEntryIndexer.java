package com.intellij.util.indexing;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.OverrideOnly
public abstract class SingleEntryIndexer<V> implements DataIndexer<Integer, V, FileContent>{
  // intentionally simplified
}