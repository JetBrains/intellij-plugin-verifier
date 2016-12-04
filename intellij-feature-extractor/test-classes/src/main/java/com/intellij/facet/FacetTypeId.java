package com.intellij.facet;

public final class FacetTypeId<F extends Facet> {
  private final String myDebugName;

  public FacetTypeId(String debugName) {
    myDebugName = debugName;
  }

  public String toString() {
    return myDebugName;
  }
}