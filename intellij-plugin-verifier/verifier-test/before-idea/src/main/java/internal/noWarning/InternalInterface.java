package internal.noWarning;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface InternalInterface {
  // This method is effectively internal.
  void someMethod();
}
