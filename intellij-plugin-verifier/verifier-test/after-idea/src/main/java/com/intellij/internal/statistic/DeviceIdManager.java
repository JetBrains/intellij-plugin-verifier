package com.intellij.internal.statistic;

public class DeviceIdManager {
  // This method must not be called from external plugins.
  public static String getOrGenerateId() {
    return "ID";
  }
}
