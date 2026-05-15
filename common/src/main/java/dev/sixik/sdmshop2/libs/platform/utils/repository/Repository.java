package dev.sixik.sdmshop2.libs.platform.utils.repository;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

public interface Repository<K, V> {
    void save(K id, V entity);

    @Nullable V load(K id);

    Map<K, V> loadAll();

    void delete(K id);

    default void setSyncCallbacks(Consumer<K> onUpdate, Consumer<K> onDelete) {

    }
}