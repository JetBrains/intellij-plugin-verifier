package mock.plugin.inheritance;

public class SubclassNoProblem extends NoProblem {
  public void baz() {
    super.foo();
  }
}
