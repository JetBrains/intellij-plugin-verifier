package mock.plugin.defaults;

import defaults.Iface;
import defaults.IfaceDefault;

public class IfaceDefaultImpl implements Iface, IfaceDefault {
  //the verifier should not complain on the method with default implementation

  public static void main(String[] args) {
    new Subclass().defaultMethod();
  }

  private static class Subclass extends IfaceDefaultImpl {
    @Override
    public void defaultMethod() {
      super.defaultMethod();
    }
  }
}
