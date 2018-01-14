package featureExtractor.fileType;

import com.intellij.openapi.fileTypes.ExactFileNameMatcher;
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;

public class MatcherFileTypeFactory extends FileTypeFactory {

  private static String FIRST_EXTENSION = "firstExactName";
  private static String SECOND_EXTENSION = "secondExactName";
  private static String THIRD_EXTENSION = "nmextension";

  @Override
  public void createFileTypes(FileTypeConsumer consumer) {
    consumer.consume(null,
        new ExactFileNameMatcher(FIRST_EXTENSION),
        new ExactFileNameMatcher(SECOND_EXTENSION, false),
        new ExtensionFileNameMatcher(THIRD_EXTENSION)
    );
  }
}
