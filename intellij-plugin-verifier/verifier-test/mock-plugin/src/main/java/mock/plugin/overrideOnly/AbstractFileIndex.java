package mock.plugin.overrideOnly;

import com.intellij.util.indexing.SingleEntryFileBasedIndexExtension;
import com.intellij.util.indexing.SingleEntryIndexer;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractFileIndex<T> extends SingleEntryFileBasedIndexExtension<T> {
    private final SingleEntryIndexer<T> indexer = new SingleEntryIndexer<T>() {
        // no implementation
    };

    /*expected(OVERRIDE_ONLY)
    Invocation of override-only method mock.plugin.overrideOnly.AbstractFileIndex.getIndexer()

    Override-only method mock.plugin.overrideOnly.AbstractFileIndex.getIndexer() is invoked in mock.plugin.overrideOnly.AbstractFileIndex.getIndexer() : DataIndexer. This method is marked with @org.jetbrains.annotations.ApiStatus.OverrideOnly annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the @ApiStatus.OverrideOnly for more info.
    */
    @NotNull
    @Override
    public SingleEntryIndexer<T> getIndexer() {
        return indexer;
    }
}