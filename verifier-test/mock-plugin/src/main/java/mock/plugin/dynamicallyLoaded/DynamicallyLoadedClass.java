package mock.plugin.dynamicallyLoaded;

import com.intellij.ide.plugins.DynamicallyLoaded;
import non.existing.NonExistingClass;

//mark this class with DynamicallyLoaded annotation to exclude it from verification
@DynamicallyLoaded
public class DynamicallyLoadedClass {
  public void foo() {
    NonExistingClass n = null;
  }
}
