package deprecated;

import org.jetbrains.annotations.ApiStatus;

public class ScheduledForRemovalMethod {

  @ApiStatus.ScheduledForRemoval(inVersion = "2018.1")
  public ScheduledForRemovalMethod() {
  }

  @ApiStatus.ScheduledForRemoval(inVersion = "2018.1")
  public void foo(int x) {
  }

  @ApiStatus.ScheduledForRemoval(inVersion = "2018.1")
  public void foo(double x) {

  }
}
