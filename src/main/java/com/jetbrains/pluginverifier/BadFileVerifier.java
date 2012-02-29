package com.jetbrains.pluginverifier;

/**
 * @author Dennis.Ushakov
 */
public class BadFileVerifier implements Verifier {
  private final String myFilename;

  public BadFileVerifier(final String filename) {
    myFilename = filename;
  }

  public void dumpErrors() {
    System.out.println(myFilename + " is corrupt");
  }

  public void verify() {
  }

  public boolean hasErrors() {
    return true;
  }
}
