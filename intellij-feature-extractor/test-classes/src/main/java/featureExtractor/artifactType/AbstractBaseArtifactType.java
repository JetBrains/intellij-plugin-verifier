package featureExtractor.artifactType;

import com.intellij.packaging.artifacts.ArtifactType;

public abstract class AbstractBaseArtifactType extends ArtifactType {
  protected AbstractBaseArtifactType(String id, String title) {
    super(id, title);
  }
}
