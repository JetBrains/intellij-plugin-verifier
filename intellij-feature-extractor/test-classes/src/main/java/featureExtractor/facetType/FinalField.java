package featureExtractor.facetType;

import com.intellij.facet.FacetType;

public class FinalField extends FacetType<SomeFacet> {
  public static final String MY_ID = "thisIsStringId";

  public FinalField() {
    super(null, MY_ID, null);
  }
}
