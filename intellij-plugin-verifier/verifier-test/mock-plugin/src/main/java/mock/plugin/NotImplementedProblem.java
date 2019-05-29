package mock.plugin;

import com.intellij.openapi.components.PersistentStateComponent;

/*expected(PROBLEM)
  Abstract method com.intellij.openapi.components.PersistentStateComponent.getState() : T is not implemented

  Concrete class mock.plugin.NotImplementedProblem inherits from com.intellij.openapi.components.PersistentStateComponent but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime.
*/
public class NotImplementedProblem implements PersistentStateComponent {

}
