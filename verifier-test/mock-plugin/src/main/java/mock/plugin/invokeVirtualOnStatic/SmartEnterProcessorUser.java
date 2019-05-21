package mock.plugin.invokeVirtualOnStatic;

import com.intellij.lang.SmartEnterProcessor;

public class SmartEnterProcessorUser extends SmartEnterProcessor {
  public void main() {
    commit(); //this method in the new IDEA became static
  }
}
