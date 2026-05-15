package dev.sixik.sdmshop2.libs.shop.base;

import dev.architectury.platform.Platform;
import dev.sixik.sdmshop2.SDMShop2;
import dev.sixik.sdmshop2.libs.platform.SDMPlatform;
import dev.sixik.sdmshop2.libs.platform.ServerOperation;
import dev.sixik.sdmshop2.libs.platform.ThreadingOperationTimeSave;
import dev.sixik.sdmshop2.libs.platform.utils.repository.RepositoryStorage;
import dev.sixik.sdmshop2.libs.platform.utils.repositoryManager.RepoDefinition;
import dev.sixik.sdmshop2.libs.platform.utils.repositoryManager.RepositoryManager;
import dev.sixik.sdmshop2.libs.shop.events.ShopServerEvents;
import dev.sixik.sdmshop2.libs.shop.scripting.events.ShopScriptEvents;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Глобальный менеджер всех магазинов в системе.
 * Отвечает за хранение, загрузку, сохранение и удаление экземпляров {@link ShopInstance}.
 */
public final class ShopTable implements ShopServerGetter{

    private static final Logger LOGGER = LoggerFactory.getLogger(ShopTable.class);

    /**
     * Глобальный экземпляр ShopTable.
     */
    public static ShopTable Instance;

    private final ExecutorService ioExecutor;
    private RepositoryStorage<ResourceLocation, ShopInstance> shopsRepository;

    /**
     * Путь к директории магазинов в папке сохранения мира.
     */
    @Getter
    private final Path shopDirWorld;

    /**
     * Путь к корневой директории настроек магазина.
     */
    @Getter
    private final Path shopDirConfig;

    /**
     * Путь к директории, где хранятся JSON-файлы магазинов.
     */
    @Getter
    private final Path shopsDir;

    /**
     * Текущий экземпляр Minecraft сервера.
     */
    @Getter
    private final MinecraftServer server;

    @Getter
    private volatile boolean reloading = false;

    private final RepositoryManager manager;

    public ShopTable(MinecraftServer server, RepositoryManager manager) {
        this.ioExecutor = Executors.newSingleThreadExecutor();
        this.server = server;
        this.shopDirWorld = SDMPlatform.resolveSdmDir(server.getWorldPath(LevelResource.ROOT), "shop");
        this.shopDirConfig = SDMPlatform.resolveSdmDir(Platform.getConfigFolder(), "shop");
        this.shopsDir = SDMPlatform.resolveSdmDir(Platform.getConfigFolder(), "shop/shops");
        this.manager = manager;
        this.manager.setServerGetter(this);
        this.manager.init();

        shopsRepository = new RepositoryStorage<>(manager.createRepository(
            shopsDir,
            SDMShop2.getDataStorageConfig().getDefaultConfig().mongodb.shopsCollection,
            new RepoDefinition<>(
                    ResourceLocation::toString,
                    ResourceLocation::new,
                    ShopInstance::getId,
                    shop -> shop.serialize().getAsJsonObject(),
                    json -> {
                        ShopInstance instance = ShopInstance.fromJson(json);
                        instance.setOnUpdate(() -> shopsRepository.update(instance.getId()));
                        return instance;
                    }
            )
        ), Object2ObjectOpenHashMap::new, ioExecutor);


        reload();
    }

    /**
     * Создает и регистрирует новый магазин.
     *
     * @param shopId Уникальный ID нового магазина
     */
    public void addShop(ResourceLocation shopId) {
        addShop(ShopInstance.createManager(shopId, true));
    }

    /**
     * Добавляет существующий экземпляр магазина в таблицу.
     *
     * @param instance Экземпляр магазина
     */
    public void addShop(ShopInstance instance) {
        shopsRepository.putValue(instance.getId(), instance);
    }

    /**
     * Возвращает экземпляр магазина по его ID.
     *
     * @param id ID искомого магазина
     * @return Экземпляр магазина или null, если не найден
     */
    @Nullable
    public ShopInstance getShop(ResourceLocation id) {
        return shopsRepository.getValue(id);
    }

    /**
     * Возвращает коллекцию всех зарегистрированных магазинов.
     *
     * @return Все магазины
     */
    public Collection<ShopInstance> getAllShops() {
        return shopsRepository.getAllValues();
    }

    /**
     * Возвращает коллекцию IDs всех зарегистрированных магазинов.
     *
     * @return Коллекция строковых ID магазинов
     */
    public List<ResourceLocation> getShopsId() {
        return shopsRepository.getAllKeys().stream().toList();
    }

    public int getShopsCount() {
        return shopsRepository.size();
    }

    /**
     * Удаляет магазин и его файл.
     *
     * @param instance Экземпляр магазина для удаления
     */
    public void deleteShop(ShopInstance instance) {
        deleteShop(instance.getId());
    }

    /**
     * Удаляет магазин по его ID и удаляет соответствующий файл JSON.
     *
     * @param id ID магазина для удаления
     */
    public void deleteShop(ResourceLocation id) {
        shopsRepository.delete(id);
    }

    /**
     * Сохраняет указанный магазин в файл.
     *
     * @param instance Экземпляр магазина
     */
    public void save(ShopInstance instance) {
        shopsRepository.save(instance.getId(), instance);
    }

    /**
     * Асинхронно сохраняет указанный магазин в файл.
     *
     * @param instance Экземпляр магазина
     */
    public void saveAsync(ShopInstance instance) {
        ioExecutor.submit(() -> save(instance));
    }

    /**
     * Перезагружает все данные магазинов из папки {@link #getShopsDir()}.
     * Все текущие данные будут перезаписанны через подмену ссылок
     */
    public void reload() {
        if(reloading) return;

        reloading = true;
        try {
            LOGGER.info("Start reloading shops data!");

            shopsRepository.loadAll();

            ShopScriptEvents.SCRIPT_SHOP_LOAD_EVENT.invoker().invoke(server, this);
            ShopServerEvents.SHOP_LOAD_EVENT.invoker().invoke(server, this);

            LOGGER.info("Loaded {} shops.", shopsRepository.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load shops", e);
        } finally {
            reloading = false;
        }
    }

    /**
     * Перезагружает данные только одного магазина из хранилища.
     */
    public void reloadShop(ResourceLocation id) {
        if (shopsRepository == null) return;
        shopsRepository.load(id);
        // TODO: Shop update event
        // ShopServerEvents.SHOP_UPDATED_EVENT.invoker().invoke(server, updatedShop);
    }

    public void shutdown() {
        manager.close();
    }

    public static class Manager implements ServerOperation, ThreadingOperationTimeSave {

        @Override
        public void onReload() {
            if(ShopTable.Instance != null) {
                ShopTable.Instance.reload();
            }
        }

        @Override
        public void onServerStart(MinecraftServer server) {
            ShopTable.Instance = new ShopTable(server, SDMShop2.getRepositoryManager(server));
        }

        @Override
        public void onServerStop(MinecraftServer server) {
            ShopTable.Instance.shutdown();
        }

        @Override
        public void onDataStartSave() { }

        @Override
        public int getDataSaveTimeSeconds() {
            return -1;
        }
    }
}
