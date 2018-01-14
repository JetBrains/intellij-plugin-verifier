package featureExtractor.fileType;

import com.intellij.openapi.fileTypes.FileType;

public class SomeFileType implements FileType {
  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public String getDefaultExtension() {
    return "mySomeExtension";
  }

  @Override
  public boolean isBinary() {
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }
}
