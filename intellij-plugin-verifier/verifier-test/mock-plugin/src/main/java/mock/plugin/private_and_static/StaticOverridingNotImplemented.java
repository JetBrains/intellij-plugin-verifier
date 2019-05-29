package mock.plugin.private_and_static;

import com.intellij.openapi.components.PersistentStateComponent;

/*expected(PROBLEM)
Abstract method com.intellij.openapi.components.PersistentStateComponent.getState() : T is not implemented

Concrete class mock.plugin.private_and_static.StaticOverridingNotImplemented inherits from com.intellij.openapi.components.PersistentStateComponent but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime.
*/
public class StaticOverridingNotImplemented implements PersistentStateComponent<String> {

  //in PSC there are 2 methods

  //should be error , because static overriding
  public static String getState() {
    return null;
  }

}
