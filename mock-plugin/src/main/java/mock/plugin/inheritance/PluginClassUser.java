package mock.plugin.inheritance;

/**
 * @author Sergey Patrikeev
 */
public class PluginClassUser {
  public void foo() {
    PluginClass pluginClass = new PluginClass();

    //verifier should not report that he has not found deletedMethod()
    //but only the fact that DeletedClass (which is a parent of PluginClass) is deleted
    //because these are related issues
    pluginClass.deletedMethod();
  }
}
