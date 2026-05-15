package dev.sixik.sdmshop2.libs.sdmeconomy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class SDMEconomyCurrencyRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(SDMEconomyCurrencyRegistry.class);

    private static final Map<ResourceLocation, ICurrencyType<?>> TYPES = new Object2ObjectOpenHashMap<>();
    private static final Map<Class<?>, ICurrencyType<?>> TYPES_BY_CLASS = new Object2ObjectOpenHashMap<>();

    private static final Map<ResourceLocation, IExternalCurrency> CURRENCIES = new Object2ObjectOpenHashMap<>();

    /**
     * Регистрирует валюту которая имеет физическую валюту.
     * @param id ID Типа
     * @param type Сериализатор
     */
    public static void registerType(ResourceLocation id, ICurrencyType<?> type) {
        TYPES.put(id, type);
        TYPES_BY_CLASS.put(type.getOwnerClass(), type);
    }

    /**
     * Регистрирует новую валюту в памяти и сохраняет её в .json файл.
     *
     * @param currency Объект валюты для сохранения
     * @param <T> Тип валюты
     * @return true если сохранение прошло успешно, иначе false
     */
    @SuppressWarnings("unchecked")
    public static <T extends IExternalCurrency> boolean registerAndSaveCurrency(T currency) {
        ResourceLocation id = currency.getId();

        CURRENCIES.put(id, currency);

        ICurrencyType<T> type = (ICurrencyType<T>) TYPES_BY_CLASS.get(currency.getClass());
        if (type == null) {
            LOGGER.error("Cannot save currency {}: No ICurrencyType registered for class {}", id, currency.getClass().getName());
            return false;
        }

        JsonObject json = type.serialize(currency);
        Path configDir = SDMEconomyPlatform.getCurrenciesDir();
        File file = configDir.resolve(id.getPath() + ".json").toFile();

        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(json, writer);

            LOGGER.info("Successfully saved currency {} to {}", id, file.getName());
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to save currency {} to {}", id, file.getName(), e);
            return false;
        }
    }

    @Nullable
    public static IExternalCurrency getCurrency(String id) {
        return getCurrency(
                        id.contains(":") ?
                        ResourceLocation.tryParse(id) :
                        ResourceLocation.tryBuild("sdm", id)
        );
    }

    @Nullable
    public static IExternalCurrency getCurrency(ResourceLocation id) {
        return CURRENCIES.get(id);
    }

    public static Map<ResourceLocation, IExternalCurrency> getCurrenciesMap() {
        return new Object2ObjectOpenHashMap<>(CURRENCIES);
    }

    public static Collection<IExternalCurrency> getCurrencies() {
        return CURRENCIES.values();
    }

    public static void forEachCurrencies(BiConsumer<ResourceLocation, IExternalCurrency> iterator) {
        CURRENCIES.forEach(iterator);
    }

    public static void reload() {
        reload(SDMEconomyPlatform.getCurrenciesDir());
    }

    /**
     * Перезагружает все валюты которые были созданы.
     * @param configDir Место где храняться пользовательские валюты
     */
    public static void reload(Path configDir) {
        CURRENCIES.clear();

        final File file = configDir.toFile();

        if(!file.exists()) {
            LOGGER.error("Can't reload currencies because, currencies folder not exists!");
            return;
        }

        final File[] listFiles = file.listFiles();

        for (int i = 0; i < listFiles.length; i++) {
            loadCurrency(listFiles[i]);
        }
    }

    private static void loadCurrency(File file) {
        final String extension = FilenameUtils.getExtension(file.getName());
        if (!extension.equals("json")) {
            LOGGER.warn("Skipping file '{}': Not a JSON", file.getName());
            return;
        }

        final String fileName = FilenameUtils.removeExtension(file.getName());
        final ResourceLocation currencyId = new ResourceLocation("sdm", fileName);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            final JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            if (!json.has("type")) {
                LOGGER.error("Failed to load currency '{}': Missing 'type' field", fileName);
                return;
            }

            final String typeStr = json.get("type").getAsString();
            final ResourceLocation typeId = new ResourceLocation(typeStr);

            final ICurrencyType<?> typeFactory = SDMEconomyCurrencyRegistry.TYPES.get(typeId);

            if (typeFactory == null) {
                LOGGER.error("Unknown currency type '{}' in file '{}'", typeId, file.getName());
                return;
            }

            final IExternalCurrency currency = typeFactory.deserialize(currencyId, json);

            CURRENCIES.put(currencyId, currency);
            LOGGER.info("Loaded currency: {}", currencyId);
        } catch (Exception e) {
            LOGGER.error("Error loading currency from file '{}'", file.getName(), e);
        }
    }

    public static CompoundTag serializeCurrencies() {
        CompoundTag nbt = new CompoundTag();

        ListTag nbtCurrencies = new ListTag();
        for (Map.Entry<ResourceLocation, IExternalCurrency> entry : CURRENCIES.entrySet()) {
            ResourceLocation key = entry.getKey();
            IExternalCurrency value = entry.getValue();
            final ICurrencyType type = TYPES_BY_CLASS.get(value.getClass());
            if (type == null) continue;

            CompoundTag data = new CompoundTag();
            data.putString("key", key.toString());
            data.putString("owner", type.getOwnerClass().getName());
            data.put("data", type.serializeNbt(value));
            nbtCurrencies.add(data);
        }

        nbt.put("currencies", nbtCurrencies);
        return nbt;
    }

    public static Object2ObjectOpenHashMap<ResourceLocation, IExternalCurrency> deserializeCurrencies(CompoundTag nbt) {
        if(!nbt.contains("currencies")) return new Object2ObjectOpenHashMap<>();

        Object2ObjectOpenHashMap<ResourceLocation, IExternalCurrency> out = new Object2ObjectOpenHashMap<>();

        ListTag nbtCurrencies = (ListTag) nbt.get("currencies");
        for (Tag nbtCurrency : nbtCurrencies) {
            CompoundTag data = (CompoundTag) nbtCurrency;

            ResourceLocation id = ResourceLocation.tryParse(data.getString("key"));
            Class<?> clz;
            try {
                clz = Class.forName(data.getString("owner"));
            } catch (ClassNotFoundException e) {
                LOGGER.error("Unknown type: {}", data.getString("owner"));
                continue;
            }

            ICurrencyType<?> type = TYPES_BY_CLASS.get(clz);
            if(type == null) {
                LOGGER.error("Unknown type: {}", clz.getName());
                continue;
            }

            IExternalCurrency obj = type.deserializeNbt(id, data.get("data"));
            out.put(id, obj);
        }

        return out;
    }
}
