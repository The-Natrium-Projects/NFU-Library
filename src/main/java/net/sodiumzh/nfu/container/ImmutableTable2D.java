package net.sodiumzh.nfu.container;

import java.util.*;
import java.util.stream.Stream;

public class ImmutableTable2D<R, C, V> extends Table2D<R, C, V> {

    public ImmutableTable2D(ITable2D.Entry<R, C, V>... entries) {
        Arrays.stream(entries).forEach(entry -> this.putInternal(entry.rowKey(), entry.columnKey(), entry.value()));
    }

    public ImmutableTable2D(ITable2D<R, C, V> from) {
        super(from);
    }

    @Override
    public void put(R row, C column, V value) {
        throw new UnsupportedOperationException("ImmutableTable2D doesn't allow modification.");
    }

    private void putInternal(R row, C column, V value) {
        super.put(row, column, value);
    }

    @Override
    public void removeValue(V value) {
        throw new UnsupportedOperationException("ImmutableTable2D doesn't allow modification.");
    }

    @Override
    public Optional<V> remove(R row, C column) {
        throw new UnsupportedOperationException("ImmutableTable2D doesn't allow modification.");
    }

    @Override
    public List<V> removeRow(R row) {
        throw new UnsupportedOperationException("ImmutableTable2D doesn't allow modification.");
    }

    @Override
    public List<V> removeColumn(C column) {
        throw new UnsupportedOperationException("ImmutableTable2D doesn't allow modification.");
    }
}
