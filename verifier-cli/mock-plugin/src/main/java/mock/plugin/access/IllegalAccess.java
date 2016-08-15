package mock.plugin.access;

import access.AccessProblemBase;

public class IllegalAccess extends AccessProblemBase {
  public static void main(String[] args) {
    AccessProblemBase problem = new AccessProblemBase();

    //will be protected. and the inheritance of this class(IllegalAccess) from AccessProblemBase doesn't matter.
    problem.foo();
    int x = problem.x;
  }
}
