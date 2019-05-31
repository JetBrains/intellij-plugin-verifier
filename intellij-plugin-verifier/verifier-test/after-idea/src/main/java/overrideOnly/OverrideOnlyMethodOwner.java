package overrideOnly;

import org.jetbrains.annotations.ApiStatus;

public class OverrideOnlyMethodOwner {
  @ApiStatus.OverrideOnly
  public void overrideOnlyMethod() {
  }
}