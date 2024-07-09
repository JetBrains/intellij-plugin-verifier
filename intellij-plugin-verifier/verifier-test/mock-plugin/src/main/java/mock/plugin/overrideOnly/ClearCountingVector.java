package mock.plugin.overrideOnly;

import java.util.Vector;

public class ClearCountingVector extends Vector<String> {
    private int clearCount = 0;

    @Override
    public void clear() {
        super.clear();
        clearCount++;
    }
}
