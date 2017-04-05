package mock.plugin.invokeVirtualOnStatic;

import com.intellij.lang.SmartEnterProcessor;

/**
 * Created by Sergey Patrikeev
 */
public class SmartEnterProcessorUser extends SmartEnterProcessor {
  public void main() {
    commit(); //this method in the new IDEA became static
  }
}
