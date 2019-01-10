package ignored;

public class B extends A<Number> {

  static {
    //this static block must be ignored.
  }


  //Bridge method generated for this method must be ignored.
  void foo(Number number) {
    super.foo(number);
  }
}
