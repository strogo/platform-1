package lsfusion.base.col.implementations.simple;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.implementations.abs.AOrderMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;

public class EmptyOrderMap<K, V> extends AOrderMap<K, V> implements ImOrderValueMap<K, V> {

    private final static EmptyOrderMap<Object, Object> instance = new EmptyOrderMap<>();
    public static <K,V> EmptyOrderMap<K, V> INSTANCE() {
        return (EmptyOrderMap<K, V>) instance;
    }
    private EmptyOrderMap() {
    }

    public int size() {
        return 0;
    }

    public K getKey(int i) {
        throw new UnsupportedOperationException();
    }

    public V getValue(int i) {
        throw new UnsupportedOperationException();
    }

    public void mapValue(int i, V value) {
        throw new UnsupportedOperationException();
    }

    public ImOrderMap<K, V> immutableValueOrder() {
        return this;
    }

    public ImMap<K, V> getMap() {
        return MapFact.EMPTY();
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return (ImOrderValueMap<K, M>) this;
    }
}
