package featureExtractor.fileType;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;

/**
 * @author Sergey Patrikeev
 */
public class ByFileTypeFactory extends FileTypeFactory {
  @Override
  public void createFileTypes(FileTypeConsumer consumer) {
    consumer.consume(new SomeFileType());
  }
}
