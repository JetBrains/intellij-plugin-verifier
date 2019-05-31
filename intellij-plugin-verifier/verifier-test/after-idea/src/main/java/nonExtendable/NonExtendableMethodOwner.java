package nonExtendable;

import org.jetbrains.annotations.ApiStatus;

public class NonExtendableMethodOwner {
  @ApiStatus.NonExtendable
  public void nonExtendableMethod() {
  }
}
