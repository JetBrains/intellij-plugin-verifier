package com.intellij.openapi.fileTypes;

public interface FileTypeConsumer {
  String EXTENSION_DELIMITER = ";";

  void consume(FileType fileType);

  void consume(FileType fileType, String extensions);

  void consume(FileType fileType, FileNameMatcher... matchers);

  FileType getStandardFileTypeByName(String name);
}
