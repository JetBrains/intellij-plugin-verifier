package mock.plugin.internal;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
class OwnInternalClass {

}

/**
 * No warnings should be produced here because {@link OwnInternalClass} is declared in the same module.
 */
public class NoWarnings {
  public void noWarning() {
    new OwnInternalClass();
  }
}

