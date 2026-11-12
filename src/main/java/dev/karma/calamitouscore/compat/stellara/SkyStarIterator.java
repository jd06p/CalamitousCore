package dev.karma.calamitouscore.compat.stellara;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/** Private immutable texture groups are consumed sequentially on Stellara's render thread. */
public final class SkyStarIterator implements Iterator<Object> {
    private List<?> group = List.of();
    private int index;
    public Iterator<Object> reset(List<?> group) { this.group = group; index = 0; return this; }
    @Override public boolean hasNext() { return index < group.size(); }
    @Override public Object next() {
        if (!hasNext()) throw new NoSuchElementException();
        return group.get(index++);
    }
}
