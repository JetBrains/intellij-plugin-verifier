package featureExtractor.fileType;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;

public class StaticInitConstantFileTypeFactory extends FileTypeFactory {

  private static final String CONSTANT = initConstant();

  private static String initConstant() {
    return "one;two;three;";
  }

  @Override
  public void createFileTypes(FileTypeConsumer consumer) {
    consumer.consume(null, CONSTANT);
  }
}
