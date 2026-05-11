package overrideOnly;

import org.jetbrains.annotations.ApiStatus;

public class OverrideOnlyMethodOwner {
  @ApiStatus.OverrideOnly
  public void overrideOnlyMethod() {
  }

  // Annotation is contradictory: a static method cannot be overridden.
  // Callers must not be flagged.
  @ApiStatus.OverrideOnly
  public static void staticOverrideOnlyMethod() {
  }
}
