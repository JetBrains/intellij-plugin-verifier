package featureExtractor.artifactType;

import com.intellij.packaging.artifacts.ArtifactType;

public class DirectArtifactType extends ArtifactType {
  protected DirectArtifactType(String id, String title) {
    super("ArtifactId", title);
  }
}
