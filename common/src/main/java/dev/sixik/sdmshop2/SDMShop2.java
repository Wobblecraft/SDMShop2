package dev.sixik.sdmshop2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.platform.Platform;
import dev.sixik.sdmshop2.libs.platform.SDMPlatform;
import dev.sixik.sdmshop2.libs.sdmeconomy.SDMEconomyPlatform;
import dev.sixik.sdmshop2.libs.sdmeconomy.commands.SDMEconomyCommands;
import dev.sixik.sdmshop2.libs.shop.base.ShopTable;
import dev.sixik.sdmshop2.libs.shop.base.limiter.ShopLimiterTableServer;
import dev.sixik.sdmshop2.libs.platform.utils.repositoryManager.JsonRepositoryManager;
import dev.sixik.sdmshop2.libs.platform.utils.repositoryManager.MongoRepositoryManager;
import dev.sixik.sdmshop2.libs.platform.utils.repositoryManager.RepositoryManager;
import dev.sixik.sdmshop2.libs.shop.commands.SDMShopCommands;
import dev.sixik.sdmshop2.libs.shop.config.ShopConfig;
import dev.sixik.sdmshop2.libs.shop.config.ShopDataStorageConfig;
import dev.sixik.sdmshop2.libs.shop.network.SDMShopNetwork;
import dev.sixik.sdmshop2.libs.shop.register.ShopRegister;
import dev.sixik.sdmshop2.libs.shop.scripting.events.ShopScriptEvents;
import dev.sixik.sdmshop2.tests.economy.EconomyTest;
import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.shadowking21.shadowconfig.config.ConfigSide;
import net.shadowking21.shadowconfig.config.exstensions.yaml.SCYamlConfig;
import org.slf4j.Logger;

public final class SDMShop2 {
    public static final String MODID = "sdmshop2";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final ShopTable.Manager SHOP_TABLE_MANAGER = new ShopTable.Manager();
    private static final ShopLimiterTableServer.Manager SHOP_LIMITER_TABLE_MANAGER = new ShopLimiterTableServer.Manager();
    private static final ShopScriptEvents.Manager SHOP_SCRIPTS_CONTAINER_MANAGER = new ShopScriptEvents.Manager();

    private static RepositoryManager instance;

    public static RepositoryManager getRepositoryManager(MinecraftServer server) {
        if(instance == null) {
            final var config = SDMShop2.getDataStorageConfig().getCurrentConfig();
            instance = switch (config.type) {
                case JSON, CUSTOM -> new JsonRepositoryManager(server);
                case MONGODB -> new MongoRepositoryManager(config.mongodb);
            };
        }

        return instance;
    }

    public static void init() {
        EconomyTest.init();

        SDMPlatform.addTask(SHOP_TABLE_MANAGER);
        SDMPlatform.addTask(SHOP_LIMITER_TABLE_MANAGER);
        SDMPlatform.addTask(SHOP_SCRIPTS_CONTAINER_MANAGER);

        SDMEconomyPlatform.init();
        ShopRegister.init();

        SDMShopNetwork.init();

        CommandRegistrationEvent.EVENT.register((s1, s2, s3) -> {
            SDMEconomyCommands.registerCommands(s1, s2, s3);
            SDMShopCommands.registerCommands(s1, s2, s3);
        });

        SDMPlatform.init();

        dataStorageConfig = (SCYamlConfig<ShopDataStorageConfig>) SCYamlConfig.Builder.builder(ShopDataStorageConfig.class)
                .defaults(new ShopDataStorageConfig())
                .modId("sdmshop")
                .side(ConfigSide.COMMON)
                .path(SDMPlatform.resolveSdmDir(Platform.getConfigFolder(), "shop"))
                .build();
        dataStorageConfig.init();
    }

    private static final ShopConfig TEMP_CONFIG = new ShopConfig();

    @Getter
    private static SCYamlConfig<ShopDataStorageConfig> dataStorageConfig;

    public static ShopConfig getConfig() {
        return TEMP_CONFIG;
    }
}
