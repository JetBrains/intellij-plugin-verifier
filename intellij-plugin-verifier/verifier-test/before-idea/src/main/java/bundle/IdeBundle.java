package bundle;

import org.jetbrains.annotations.PropertyKey;

public class IdeBundle {

  private static final String BUNDLE_NAME = "messages.IdeBundle";

  public static String getMessage(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... values) {
    return null;
  }
}
