package com.intellij.facet;

public abstract class FacetType<F extends Facet> {

  private final String myStringId;

  public FacetType(final FacetTypeId<F> id, final String stringId, final String presentableName,
                   final FacetTypeId underlyingFacetType) {
    myStringId = stringId;
  }

  public FacetType(final FacetTypeId<F> id, final String stringId, final String presentableName) {
    this(null, stringId, null, null);
  }

  public String getStringId() {
    return myStringId;
  }
}