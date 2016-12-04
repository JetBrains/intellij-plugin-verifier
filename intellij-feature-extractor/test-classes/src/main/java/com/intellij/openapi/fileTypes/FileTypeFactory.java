package com.intellij.openapi.fileTypes;

public abstract class FileTypeFactory {

  public abstract void createFileTypes(FileTypeConsumer consumer);

}