package com.jetbrains.pluginverifier;

/**
 * @author Dennis.Ushakov
 */
public interface Verifier {
  public void dumpErrors();
  public void verify();

  public boolean hasErrors();
}
