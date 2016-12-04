package featureExtractor;

/**
 * @author Sergey Patrikeev
 */
public class IndirectArtifactType extends AbstractBaseArtifactType {

  private static final String CONSTANT = "IndirectArtifactId";

  protected IndirectArtifactType(String id, String title) {
    super(CONSTANT, title);
  }
}
