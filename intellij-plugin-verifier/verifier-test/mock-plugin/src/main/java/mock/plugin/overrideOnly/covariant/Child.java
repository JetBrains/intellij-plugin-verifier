package mock.plugin.overrideOnly.covariant;

public class Child extends Parent {
    @Override
    public ChildResult provide() {
        return new ChildResult();
    }
}
