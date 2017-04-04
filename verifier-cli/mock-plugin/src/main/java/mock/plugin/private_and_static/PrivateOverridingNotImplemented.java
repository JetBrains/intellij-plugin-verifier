package mock.plugin.private_and_static;

import com.intellij.openapi.components.PersistentStateComponent;

public class PrivateOverridingNotImplemented implements PersistentStateComponent<String> {

  //should be error , because private overriding
  private String getState() {
    return null;
  }

}
