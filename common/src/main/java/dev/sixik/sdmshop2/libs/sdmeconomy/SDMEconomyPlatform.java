package dev.sixik.sdmshop2.libs.sdmeconomy;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.sixik.sdmshop2.libs.platform.SDMPlatform;
import dev.sixik.sdmshop2.libs.platform.ServerOperation;
import dev.sixik.sdmshop2.libs.platform.utils.repositoryManager.JsonRepositoryManager;
import dev.sixik.sdmshop2.libs.platform.utils.repositoryManager.MongoRepositoryManager;
import dev.sixik.sdmshop2.libs.platform.utils.repositoryManager.RepositoryManager;
import dev.sixik.sdmshop2.libs.sdmeconomy.config.SDMEconomyDataStorageConfig;
import dev.sixik.sdmshop2.libs.sdmeconomy.custom_currency.ExternalItemCurrency;
import dev.sixik.sdmshop2.libs.sdmeconomy.network.SDMEconomyNetwork;
import dev.sixik.sdmshop2.libs.sdmeconomy.network.packets.SendDynamicCurrencyS2C;
import dev.sixik.sdmshop2.libs.sdmeconomy.network.packets.SendPlayerAccountS2C;
import dev.sixik.sdmshop2.utils.exceptions.NotInitializedException;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.shadowking21.shadowconfig.config.ConfigSide;
import net.shadowking21.shadowconfig.config.exstensions.yaml.SCYamlConfig;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class SDMEconomyPlatform {

    @Nullable
    public static MinecraftServer server;

    public static final String MODID = "sdmeconomy";

    private static Path CONFIG_DIR;
    private static Path CURRENCIES_DIR;
    private static Path PLAYERS_DATA_DIR;

    @Getter
    private static SCYamlConfig<SDMEconomyDataStorageConfig> dataStorageConfig;

    private static RepositoryManager instance;

    public static RepositoryManager getRepositoryManager(MinecraftServer server) {
        if (instance == null) {
            final var config = SDMEconomyPlatform.getDataStorageConfig().getCurrentConfig();
            instance = switch (config.type) {
                case JSON, CUSTOM -> new JsonRepositoryManager(server);
                case MONGODB -> new MongoRepositoryManager(config.mongodb.uri, config.mongodb.database, config.mongodb.serverName);
            };
        }

        return instance;
    }

    public static void loadConfigDir(Path path) {
        CONFIG_DIR = SDMPlatform.resolveSdmDir(path, "economy");
        CURRENCIES_DIR = SDMPlatform.resolveSdmDir(path, "economy/currencies");
    }

    public static void loadPlayersDataDir(Path path) {
        PLAYERS_DATA_DIR = SDMPlatform.resolveSdmDir(path, "economy/players");
    }

    public static Path getConfigDir() {
        if (CONFIG_DIR == null)
            throw new NotInitializedException("CONFIG_DIR");

        return CONFIG_DIR;
    }

    public static Path getPlayersDataDir() {
        if (PLAYERS_DATA_DIR == null)
            throw new NotInitializedException("PLAYERS_DATA_DIR");

        return PLAYERS_DATA_DIR;
    }

    public static Path getCurrenciesDir() {
        if (CURRENCIES_DIR == null)
            throw new NotInitializedException("CURRENCIES_DIR");

        return CURRENCIES_DIR;
    }

    public static void init() {
        dataStorageConfig = (SCYamlConfig<SDMEconomyDataStorageConfig>) SCYamlConfig.Builder.builder(SDMEconomyDataStorageConfig.class)
                .defaults(new SDMEconomyDataStorageConfig())
                .modId(MODID)
                .side(ConfigSide.COMMON)
                .path(SDMPlatform.resolveSdmDir(Platform.getConfigFolder(), "economy"))
                .build();
        dataStorageConfig.init();

        SDMPlatform.addOperation(new ServerOperation() {
            @Override
            public void onServerStart(MinecraftServer server) {
                SDMEconomyPlatform.onServerStart(server);
            }

            @Override
            public void onServerStop(MinecraftServer server) {
                SDMEconomyPlatform.onServerStop(server);
            }

            @Override
            public void onReload() {
                SDMEconomyPlatform.onReload();
            }
        });

        SDMEconomyPlatform.loadConfigDir(Platform.getConfigFolder());

        SDMEconomyCurrencyRegistry.registerType(ResourceLocation.tryBuild("minecraft", "item"), ExternalItemCurrency.TYPE);
        shutdownHook();

        SDMEconomyNetwork.init();
        PlayerEvent.PLAYER_JOIN.register(SDMEconomyPlatform::onPlayerJoin);
        PlayerEvent.PLAYER_QUIT.register(SDMEconomyPlatform::onPlayerLeft);
    }

    public static void onServerStart(MinecraftServer server) {
        SDMEconomyPlatform.server = server;
        loadPlayersDataDir(server.getWorldPath(LevelResource.ROOT));
        SDMEconomyService.init(server, getRepositoryManager(server));
    }

    public static void onServerStop(MinecraftServer server) {
        SDMEconomyService.getInstance().saveAllDirty();
        SDMEconomyPlatform.server = null;
    }

    public static void onPlayerLeft(ServerPlayer player) {
        SDMEconomyService.getInstance().unloadPlayer(player.getGameProfile().getId());
    }

    public static void shutdownHook() {
        final Thread thread = new Thread(() -> {
            SDMEconomyService.getInstance().saveAllDirty();
            SDMEconomyPlatform.server = null;
        });
        thread.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(thread);
    }

    public static void onReload() {

        SDMEconomyCurrencyRegistry.reload();
        if (server == null) return;
        final CompoundTag nbt = SDMEconomyCurrencyRegistry.serializeCurrencies();
        final SendDynamicCurrencyS2C packet = new SendDynamicCurrencyS2C(nbt);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            packet.sendTo(player);
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        new SendDynamicCurrencyS2C().sendTo(player);
        new SendPlayerAccountS2C(player).sendTo(player);
    }
}
