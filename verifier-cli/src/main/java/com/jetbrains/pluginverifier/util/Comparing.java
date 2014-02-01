package com.jetbrains.pluginverifier.util;

import org.jetbrains.annotations.Nullable;

public class Comparing {

  public static boolean equal(@Nullable String arg1, @Nullable String arg2) {
    return arg1 == null ? arg2 == null : arg1.equals(arg2);
  }


}
