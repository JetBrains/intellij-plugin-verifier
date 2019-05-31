package overrideOnly;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.OverrideOnly
public interface AllOverrideOnlyMethodsOwner {
  void overrideOnlyMethod();
}