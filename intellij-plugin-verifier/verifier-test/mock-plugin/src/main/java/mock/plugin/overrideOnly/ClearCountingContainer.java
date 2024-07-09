package mock.plugin.overrideOnly;

public class ClearCountingContainer extends Container {
    private int clearCount = 0;

    @Override
    public void clear() {
        super.clear();
        clearCount++;
    }
}
