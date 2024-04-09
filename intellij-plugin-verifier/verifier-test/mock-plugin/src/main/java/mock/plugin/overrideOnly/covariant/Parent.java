package mock.plugin.overrideOnly.covariant;

public class Parent {
    @SuppressWarnings("unused")
    public ParentResult provide() {
        return new ParentResult();
    }
}
