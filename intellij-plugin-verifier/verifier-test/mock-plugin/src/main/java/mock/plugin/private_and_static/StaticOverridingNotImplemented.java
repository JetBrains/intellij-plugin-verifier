package mock.plugin.private_and_static;

import com.intellij.openapi.components.PersistentStateComponent;

public class StaticOverridingNotImplemented implements PersistentStateComponent<String> {

  //in PSC there are 2 methods

  //should be error , because static overriding
  public static String getState() {
    return null;
  }

}
