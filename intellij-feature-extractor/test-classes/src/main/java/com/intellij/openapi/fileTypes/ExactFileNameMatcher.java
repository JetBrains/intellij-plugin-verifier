package com.intellij.openapi.fileTypes;

public class ExactFileNameMatcher implements FileNameMatcher {
  private final String myFileName;
  private final boolean myIgnoreCase;

  public ExactFileNameMatcher(final String fileName) {
    myFileName = fileName;
    myIgnoreCase = false;
  }

  public ExactFileNameMatcher(final String fileName, final boolean ignoreCase) {
    myFileName = fileName;
    myIgnoreCase = ignoreCase;
  }

}