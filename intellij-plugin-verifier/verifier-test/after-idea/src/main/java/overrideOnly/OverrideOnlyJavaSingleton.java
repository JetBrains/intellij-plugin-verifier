package overrideOnly;

import org.jetbrains.annotations.ApiStatus;

/**
 * Java equivalent of a Kotlin `object`: a {@code final} singleton exposed through a
 * static {@code INSTANCE} field. The class cannot be subclassed, so callers cannot
 * override {@link #overrideOnlyMethod()} and the {@code @OverrideOnly} annotation is
 * just as unenforceable as it is on a Kotlin object. Callers must not be flagged.
 */
public final class OverrideOnlyJavaSingleton {
  public static final OverrideOnlyJavaSingleton INSTANCE = new OverrideOnlyJavaSingleton();

  private OverrideOnlyJavaSingleton() {
  }

  @ApiStatus.OverrideOnly
  public void overrideOnlyMethod() {
  }
}
