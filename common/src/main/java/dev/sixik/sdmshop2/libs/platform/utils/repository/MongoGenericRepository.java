package dev.sixik.sdmshop2.libs.platform.utils.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import dev.sixik.sdmshop2.libs.platform.utils.repositoryManager.MongoRepositoryManager;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class MongoGenericRepository<K, V> implements Repository<K, V>, MongoRepositoryManager.MongoChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoGenericRepository.class);

    private final MongoCollection<Document> collection;
    private final String serverIdentifier;

    private final Function<String, K> stringToKey;
    private final Function<K, String> keyToString;
    private final Function<V, K> extractKey;
    private final Function<V, JsonObject> serializer;
    private final Function<JsonObject, V> deserializer;

    private Consumer<K> onUpdateCallback;
    private Consumer<K> onDeleteCallback;

    public MongoGenericRepository(
            MongoRepositoryManager manager,
            String collectionName,
            Function<K, String> keyToString,
            Function<String, K> stringToKey,
            Function<V, K> extractKey,
            Function<V, JsonObject> serializer,
            Function<JsonObject, V> deserializer
    ) {
        MongoRepositoryManager.MongoRef ref = manager.createRef(collectionName, this);
        this.collection = ref.collection();
        this.serverIdentifier = manager.getServerIdentifier();
        this.keyToString = keyToString;
        this.stringToKey = stringToKey;
        this.extractKey = extractKey;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    public Map<K, V> createMap() {
        return new ConcurrentHashMap<>();
    }

    @Override
    public void save(K id, V entity) {
        String stringId = keyToString.apply(id);
        JsonObject json = serializer.apply(entity);

        Document doc = Document.parse(json.toString());
        doc.put("_id", stringId);
        doc.put("last_updated_by", serverIdentifier);

        collection.replaceOne(
                Filters.eq("_id", stringId),
                doc,
                new ReplaceOptions().upsert(true)
        );
    }

    @Override
    public @Nullable V load(K id) {
        Document doc = collection.find(Filters.eq("_id", keyToString.apply(id))).first();
        if (doc == null) return null;

        try {
            JsonObject json = JsonParser.parseString(doc.toJson()).getAsJsonObject();
            return deserializer.apply(json);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<K, V> loadAll() {
        Map<K, V> map = createMap();
        for (Document doc : collection.find()) {
            try {
                JsonObject json = JsonParser.parseString(doc.toJson()).getAsJsonObject();
                V entity = deserializer.apply(json);
                map.put(extractKey.apply(entity), entity);
            } catch (Exception e) {
                LOGGER.error("Failed to deserialize entity from MongoDB. Doc ID: {}", doc.getString("_id"), e);
            }
        }
        return map;
    }

    @Override
    public void delete(K id) {
        collection.deleteOne(Filters.eq("_id", keyToString.apply(id)));
    }

    @Override
    public void setSyncCallbacks(Consumer<K> onUpdate, Consumer<K> onDelete) {
        this.onUpdateCallback = onUpdate;
        this.onDeleteCallback = onDelete;
    }

    @Override
    public void onRemoteUpdate(String rawId) {
        if (onUpdateCallback != null) {
            onUpdateCallback.accept(stringToKey.apply(rawId));
        }
    }

    @Override
    public void onRemoteDelete(String rawId) {
        if (onDeleteCallback != null) {
            onDeleteCallback.accept(stringToKey.apply(rawId));
        }
    }
}
