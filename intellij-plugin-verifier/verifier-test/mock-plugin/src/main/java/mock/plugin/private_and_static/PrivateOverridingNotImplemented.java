package mock.plugin.private_and_static;

import com.intellij.openapi.components.PersistentStateComponent;

/*expected(PROBLEM)
Abstract method com.intellij.openapi.components.PersistentStateComponent.getState() : T is not implemented

Concrete class mock.plugin.private_and_static.PrivateOverridingNotImplemented inherits from com.intellij.openapi.components.PersistentStateComponent but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime.
*/
public class PrivateOverridingNotImplemented implements PersistentStateComponent<String> {

  //should be error , because private overriding
  private String getState() {
    return null;
  }

}
