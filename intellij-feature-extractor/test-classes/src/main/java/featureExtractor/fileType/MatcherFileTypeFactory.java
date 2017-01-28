package featureExtractor.fileType;

import com.intellij.openapi.fileTypes.ExactFileNameMatcher;
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;

/**
 * @author Sergey Patrikeev
 */
public class MatcherFileTypeFactory extends FileTypeFactory {

  private static String CONSTANT = "fnmconstant";

  @Override
  public void createFileTypes(FileTypeConsumer consumer) {
    consumer.consume(null,
        new ExactFileNameMatcher("firstExactName"),
        new ExactFileNameMatcher("secondExactName", false),
        new ExtensionFileNameMatcher("nmextension")
    );
  }
}
