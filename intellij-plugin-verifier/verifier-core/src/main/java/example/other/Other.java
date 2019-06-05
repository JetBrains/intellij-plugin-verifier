package example.other;

import example.Example;

public class Other extends Example {

  public void publicFoo() {
    Example.staticFoo();
  }

  public void bar(Example e) {
//    e.foo();
  }
}