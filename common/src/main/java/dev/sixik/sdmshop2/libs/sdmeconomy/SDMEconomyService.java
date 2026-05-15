package dev.sixik.sdmshop2.libs.sdmeconomy;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.architectury.platform.Platform;
import dev.sixik.sdmshop2.libs.platform.SDMPlatform;
import dev.sixik.sdmshop2.libs.platform.utils.repository.RepositoryStorage;
import dev.sixik.sdmshop2.libs.platform.utils.repositoryManager.RepoDefinition;
import dev.sixik.sdmshop2.libs.platform.utils.repositoryManager.RepositoryManager;
import dev.sixik.sdmshop2.libs.shop.base.ShopServerGetter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Класс в которм храняться данные о всех игроках и их балансах. <br> <br>
 * <p>Основной принцип работы такой что если игрока в течении 10 минут не используют он будет выгружен на диск
 * , но если он понадобиться он будет обратно загружен</p>
 */
public class SDMEconomyService implements ShopServerGetter {

    public static final Logger LOGGER = LoggerFactory.getLogger(SDMEconomyService.class);

    @Getter
    private static SDMEconomyService Instance;

    public static SDMEconomyService init(MinecraftServer server, RepositoryManager manager) {
        return init(new SDMEconomyService(server, manager));
    }

    public static SDMEconomyService init(SDMEconomyService service) {
        Instance = service;
        return service;
    }

    protected RepositoryStorage<UUID, BankAccount> accountRepository;
    protected final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    @Getter
    private Path dataFolder;

    private MinecraftServer server;

    public SDMEconomyService() {
        this(null, null);
    }

    public SDMEconomyService(MinecraftServer server, RepositoryManager manager) {
        if(manager == null) return;
        this.server = server;
        manager.setServerGetter(this);
        manager.init();
        this.dataFolder = SDMPlatform.resolveSdmDir(Platform.getConfigFolder(), "economy/accounts");
        this.accountRepository = new RepositoryStorage<>(manager.createRepository(
                dataFolder,
                SDMEconomyPlatform.getDataStorageConfig().getCurrentConfig().mongodb.accountsCollection,
                new RepoDefinition<>(
                        UUID::toString,
                        UUID::fromString,
                        BankAccount::getGameProfileOwnerId,
                        BankAccount::serializeJson,
                        json -> {
                            BankAccount obj = BankAccount.deserializeJson(json);
                            obj.setOnUpdate(() -> accountRepository.update(obj.getGameProfileOwnerId()));
                            return obj;
                        }
                )
        ), Object2ObjectOpenHashMap::new, ioExecutor);
    }

    /**
     * Получает данные игрока с его валютами. <br>
     * <p>Если игрока нет в кэше он будет загружен с диска</p>
     * @param gameProfileId {@link com.mojang.authlib.GameProfile}
     */
    public BankAccount getAccount(UUID gameProfileId) {
        return accountRepository.getOrCreate(gameProfileId, (player) -> {
            BankAccount obj = new BankAccount(player);
            obj.setOnUpdate(() -> accountRepository.update(obj.getGameProfileOwnerId()));
            return obj;
        });
    }

    /**
     * Выгружает игрока из памяти на диск если он уже не нужно
     * @param gameProfileId {@link com.mojang.authlib.GameProfile}
     */
    public void unloadPlayer(UUID gameProfileId) {
        BankAccount account = accountRepository.getValue(gameProfileId);
        if (account != null && account.isDirty()) {
            accountRepository.save(gameProfileId, account);
            account.markClean();
        }

        accountRepository.unload(gameProfileId);
    }

    /**
     * Сохраняет все Аккаунты нуждающиеся в этом
     */
    public void saveAllDirty() {
        ioExecutor.submit(() -> {
            for (BankAccount acc : accountRepository.getAllValues()) {
                if (acc.isDirty()) {
                    accountRepository.save(acc.getGameProfileOwnerId(), acc);
                    acc.markClean();
                }
            }
        });
    }

    @Override
    public Path getShopDirWorld() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path getShopDirConfig() {
        return dataFolder;
    }

    @Override
    public Path getShopsDir() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MinecraftServer getServer() {
        return server;
    }
}
