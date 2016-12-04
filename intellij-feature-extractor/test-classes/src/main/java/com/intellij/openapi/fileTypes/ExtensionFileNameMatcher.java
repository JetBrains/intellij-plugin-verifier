package com.intellij.openapi.fileTypes;

public class ExtensionFileNameMatcher implements FileNameMatcher {
  private final String myExtension;
  private final String myDotExtension;

  public ExtensionFileNameMatcher(String extension) {
    myExtension = extension.toLowerCase();
    myDotExtension = "." + myExtension;
  }

}
