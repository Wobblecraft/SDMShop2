package dev.sixik.sdmshop2.libs.platform.utils.repository;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RepositoryStorage<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryStorage.class);

    private final Repository<K, V> repository;
    private final Supplier<Map<K, V>> mapFactory;
    private final ExecutorService ioExecutor;

    private final Object writeLock = new Object();

    private volatile Map<K, V> storage;

    public RepositoryStorage(Repository<K, V> repository, Supplier<Map<K, V>> createMap, ExecutorService ioExecutor) {
        this.repository = repository;
        this.mapFactory = createMap;
        this.storage = createMap.get();
        this.ioExecutor = ioExecutor;

        this.repository.setSyncCallbacks(
                this::reloadFromRemote,
                this::removeLocalCache
        );
    }

    public void load(K key) {
        final V value = repository.load(key);
        if(value == null) return;
        storage.put(key, value);
    }

    /**
     * Безопасная перезагрузка.
     * Игроки не заметят фризов, так как старая карта работает до момента полной загрузки новой.
     */
    public void loadAll() {
        Map<K, V> newMap = mapFactory.get();
        newMap.putAll(repository.loadAll());

        synchronized (writeLock) {
            this.storage = newMap; // Атомарная подмена ссылки
        }
    }

    /**
     * Мгновенно обновляет кэш и асинхронно отправляет в БД.
     */
    public void putValue(K key, V value) {
        storage.put(key, value);

        ioExecutor.submit(() -> {
            try {
                repository.save(key, value);
            } catch (Exception e) {
                LOGGER.error("Failed to async save value for key: {}", key, e);
            }
        });
    }

    public void update(K key) {
        V value = storage.get(key);
        if (value != null) {
            ioExecutor.submit(() -> {
                try {
                    repository.save(key, value);
                } catch (Exception e) {
                    LOGGER.error("Failed to async update value for key: {}", key, e);
                }
            });
        }
    }

    /**
     * Вызывается ТОЛЬКО слушателем MongoDB (Change Streams) для обновления кэша
     * при изменениях на других серверах.
     */
    public void reloadFromRemote(K key) {
        V updatedValue = repository.load(key);
        if (updatedValue != null) {
            storage.put(key, updatedValue);
        } else {
            storage.remove(key);
        }
    }

    public void removeLocalCache(K key) {
        storage.remove(key);
    }

    @Nullable
    public V getValue(K key) {
        return storage.computeIfAbsent(key, repository::load);
    }

    public V getOrCreate(K key, Function<K, V> entityFactory) {
        return storage.computeIfAbsent(key, k -> {
            /*
                Пытаемся загрузить из базы данных
             */
            V loadedFromDb = repository.load(k);
            if (loadedFromDb != null) {
                return loadedFromDb;
            }

            /*
                Если в БД пусто - создаем новый через переданную фабрику
             */
            V newEntity = entityFactory.apply(k);

            ioExecutor.submit(() -> {
                try {
                    repository.save(k, newEntity);
                } catch (Exception e) {
                    LOGGER.error("Failed to async save new entity for key: {}", k, e);
                }
            });

            return newEntity;
        });
    }

    public Collection<V> getAllValues() {
        return storage.values();
    }

    public Collection<K> getAllKeys() {
        return storage.keySet();
    }

    public Map<K, V> getMap() {
        return new Object2ObjectOpenHashMap<>(storage);
    }

    /**
     * Мгновенно удаляет из кэша и асинхронно удаляет из БД.
     */
    public void delete(K key) {
        V oldData = storage.remove(key);

        ioExecutor.submit(() -> {
            try {
                repository.delete(key);
            } catch (Exception e) {
                LOGGER.error("Failed to async delete value for key: {}", key, e);

                if (oldData != null)
                    storage.put(key, oldData);
            }
        });
    }

    public int size() {
        return storage.size();
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        storage.forEach(action);
    }

    public void save(K key, V value) {
        if(storage.get(key) != null)
            repository.save(key, value);
    }


    public boolean unload(K key) {
        return storage.remove(key) != null;
    }
}
