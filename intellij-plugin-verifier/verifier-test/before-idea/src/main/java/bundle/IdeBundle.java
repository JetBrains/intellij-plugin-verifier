package bundle;

import org.jetbrains.annotations.PropertyKey;

public class IdeBundle {

  private static final String BUNDLE_NAME = "messages.IdeBundle";

  public static String getMessage(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... values) {
    // No need to implement the correct logic here. It would be as follows:
    // 1) If the "key" is in the messages.IdeBundle, then return its value.
    // 2) If the "key" is in the CoreDeprecatedMessagesBundle, then return the deprecated value.
    // 3) Otherwise throw an exception that the property is unknown.
    return null;
  }
}
