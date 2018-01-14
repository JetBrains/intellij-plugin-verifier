package featureExtractor.fileType;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import featureExtractor.common.ConstantHolder;

public class ConstantFunctionFileTypeFactory extends FileTypeFactory {
  @Override
  public void createFileTypes(FileTypeConsumer consumer) {
    consumer.consume(null, new ConstantHolder().myFunction());
  }
}


