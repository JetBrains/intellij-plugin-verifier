package mock.plugin.overrideOnly;

import com.intellij.util.indexing.SingleEntryFileBasedIndexExtension;
import com.intellij.util.indexing.SingleEntryIndexer;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractFileIndex<T> extends SingleEntryFileBasedIndexExtension<T> {
    private final SingleEntryIndexer<T> indexer = new SingleEntryIndexer<T>() {
        // no implementation
    };

    @NotNull
    @Override
    public SingleEntryIndexer<T> getIndexer() {
        return indexer;
    }
}