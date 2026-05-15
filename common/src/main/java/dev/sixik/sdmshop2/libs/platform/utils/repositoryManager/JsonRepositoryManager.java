package dev.sixik.sdmshop2.libs.platform.utils.repositoryManager;

import dev.architectury.platform.Platform;
import dev.sixik.sdmshop2.libs.platform.SDMPlatform;
import dev.sixik.sdmshop2.libs.sdmeconomy.SDMEconomyPlatform;
import dev.sixik.sdmshop2.libs.shop.base.ShopServerGetter;
import dev.sixik.sdmshop2.libs.platform.utils.repository.JsonGenericRepository;
import dev.sixik.sdmshop2.libs.platform.utils.repository.Repository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;

public class JsonRepositoryManager extends RepositoryManager{

    public JsonRepositoryManager(MinecraftServer server) {
        setServerGetter(new ShopServerGetter() {
            @Override
            public Path getShopDirWorld() {
                return SDMPlatform.resolveSdmDir(server.getWorldPath(LevelResource.ROOT), "shop");
            }

            @Override
            public Path getShopDirConfig() {
                return SDMPlatform.resolveSdmDir(Platform.getConfigFolder(), "shop");
            }

            @Override
            public Path getShopsDir() {
                return SDMPlatform.resolveSdmDir(Platform.getConfigFolder(), "shop/shops");
            }

            @Override
            public MinecraftServer getServer() {
                return server;
            }
        });
    }

    @Override
    public void init() {

    }

    @Override
    public void close() {

    }

    @Override
    public <K, V> Repository<K, V> createRepository(@Nullable Path custom, @NonNull String name, RepoDefinition<K, V> def) {
        Path collectionPath = custom == null ? serverGetter.getShopDirConfig().resolve(name) : custom;
        return new JsonGenericRepository<>(
                collectionPath,
                def.keyToString(),
                def.extractKey(),
                def.serializer(),
                def.deserializer()
        );
    }
}
