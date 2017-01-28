package featureExtractor.artifactType;

import com.intellij.packaging.artifacts.ArtifactType;

/**
 * @author Sergey Patrikeev
 */
public abstract class AbstractBaseArtifactType extends ArtifactType {
  protected AbstractBaseArtifactType(String id, String title) {
    super(id, title);
  }
}
