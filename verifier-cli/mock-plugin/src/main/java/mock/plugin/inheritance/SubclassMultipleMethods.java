package mock.plugin.inheritance;

public class SubclassMultipleMethods extends MultipleMethods {
  public void baz() {
    super.foo();
  }
}
