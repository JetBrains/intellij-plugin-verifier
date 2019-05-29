package mock.plugin.invokeVirtualOnStatic;

import com.intellij.lang.SmartEnterProcessor;

/*expected(PROBLEM)
  Attempt to execute instance instruction *invokevirtual* on a static method com.intellij.lang.SmartEnterProcessor.commit() : void

  Method mock.plugin.invokeVirtualOnStatic.SmartEnterProcessorUser.main() : void contains an *invokevirtual* instruction referencing a static method com.intellij.lang.SmartEnterProcessor.commit() : void, what might have been caused by incompatible change of the method to static. This can lead to **IncompatibleClassChangeError** exception at runtime.
*/
public class SmartEnterProcessorUser extends SmartEnterProcessor {
  public void main() {
    commit(); //this method in the new IDEA became static
  }
}
