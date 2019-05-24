package deprecated;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.ScheduledForRemoval(inVersion = "2018.1")
public interface ScheduledForRemovalInterface {
  @ApiStatus.ScheduledForRemoval(inVersion = "2018.1")
  void bar();
}
