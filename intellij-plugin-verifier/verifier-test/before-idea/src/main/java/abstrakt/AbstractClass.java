package abstrakt;

public abstract class AbstractClass {
  // Will be changed: Child -> Parent
  public abstract Child foo();

  public void invoke() {
    foo();
  }
}
