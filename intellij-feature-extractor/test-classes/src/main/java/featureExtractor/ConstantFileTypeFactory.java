package featureExtractor;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;

/**
 * @author Sergey Patrikeev
 */
public class ConstantFileTypeFactory extends FileTypeFactory {

  @Override
  public void createFileTypes(FileTypeConsumer consumer) {
    consumer.consume(null, ".someExtension");
  }
}
