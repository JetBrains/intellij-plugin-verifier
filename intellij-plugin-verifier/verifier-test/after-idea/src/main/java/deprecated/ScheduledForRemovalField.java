package deprecated;

import org.jetbrains.annotations.ApiStatus;

public class ScheduledForRemovalField {
  @ApiStatus.ScheduledForRemoval(inVersion = "2018.1")
  public int x;

  @ApiStatus.ScheduledForRemoval(inVersion = "2018.1")
  public int y;
}
