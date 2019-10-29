package featureExtractor.fileType;

import com.intellij.openapi.fileTypes.ExactFileNameMatcher;
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;

public class MatcherFileTypeFactory extends FileTypeFactory {

  private static final String FIRST_EXTENSION;
  private static final String SECOND_EXTENSION;
  private static final String THIRD_EXTENSION;

  static {
    FIRST_EXTENSION = "firstExactName";
    SECOND_EXTENSION = "secondExactName";
    THIRD_EXTENSION = "nmextension";
  }

  @Override
  public void createFileTypes(FileTypeConsumer consumer) {
    consumer.consume(null,
        new ExactFileNameMatcher(FIRST_EXTENSION),
        new ExactFileNameMatcher(SECOND_EXTENSION, false),
        new ExtensionFileNameMatcher(THIRD_EXTENSION)
    );
  }
}
