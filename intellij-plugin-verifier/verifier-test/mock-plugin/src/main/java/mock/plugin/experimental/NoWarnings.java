package mock.plugin.experimental;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
class OwnExperimentalClass {

}

/**
 * No warnings should be produced here because {@link OwnExperimentalClass} is declared in the same module.
 */
public class NoWarnings {
  public void noWarning() {
    new OwnExperimentalClass();
  }
}
