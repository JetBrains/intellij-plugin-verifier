package ignored;

public class B extends A<Number> {

  static {
    //this static block must be ignored.
  }

  //Overriding method must be ignored.
  @Override
  public void bar() {
  }

  //Bridge method generated for this method must be ignored,
  //but foo(Number) itself must be recorded because it has generified parameter
  //and A.foo() has signature foo(Object) in bytecode.
  @Override
  public void foo(Number number) {
    super.foo(number);
  }


  //Overriding toString() must be ignored.
  @Override
  public String toString() {
    return "";
  }
}
