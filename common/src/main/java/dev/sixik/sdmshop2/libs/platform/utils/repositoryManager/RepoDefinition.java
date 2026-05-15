package dev.sixik.sdmshop2.libs.platform.utils.repositoryManager;

import com.google.gson.JsonObject;

import java.util.function.Function;

public record RepoDefinition<K, V>(
        Function<K, String> keyToString,   // K -> String
        Function<String, K> stringToKey,   // String -> K (для синхронизации)
        Function<V, K> extractKey,         // V -> K
        Function<V, JsonObject> serializer,
        Function<JsonObject, V> deserializer
) {}