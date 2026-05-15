package dev.sixik.sdmshop2.libs.shop.components.misc;

import com.google.gson.JsonObject;
import dev.sixik.sdmshop2.libs.shop.base.ShopOffer;
import dev.sixik.sdmshop2.libs.shop.components.api.IComponentType;
import dev.sixik.sdmshop2.libs.shop.components.api.ShopComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShopCategoriesContainerComponent extends ShopComponent {

    public static final IComponentType<ShopCategoriesContainerComponent> TYPE = new Type();

    protected final Map<UUID, List<ShopOffer>> indexedEntries = new ConcurrentHashMap<>();

    @Override
    public IComponentType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldSync() {
        return false;
    }

    @Override
    public void init() {
        reindex();
    }

    /**
     * Пересоздаёт кэш категорий. Так как Товары храняться в плоском массиве где доступ к Элементу O(N) <br>
     * А кэш позволяет получить отсортированный товары по категории
     */
    public void reindex() {
        Optional<ShopOffersContainerComponent> opt = getRoot().getComponent(ShopOffersContainerComponent.class);
        if(opt.isEmpty()) return;

        ShopOffersContainerComponent container = opt.get();

        indexedEntries.clear();
        for (ShopOffer entry : container.getEntryMap().values()) {
            Optional<CatalogComponent> opt2 = entry.getComponent(CatalogComponent.class);

            if(opt2.isEmpty()) throw new NullPointerException("ShopEntry didn't have 'CategoryComponent'!");

            CatalogComponent categoryComponent = opt2.get();
            indexedEntries.computeIfAbsent(categoryComponent.getUuid(), (id) -> new ArrayList<>())
                    .add(entry);
        }
    }

    /**
     * Возвращает копию массива UUID ключей категорий
     */
    public List<UUID> getCategorise() {
        return new ArrayList<>(indexedEntries.keySet());
    }

    /**
     * Возвращает копию массива Товаров категории
     */
    public List<ShopOffer> getCategoriesEntry(UUID categoryId) {
        return new ArrayList<>(indexedEntries.getOrDefault(categoryId, new ArrayList<>()));
    }

    private static class Type implements IComponentType<ShopCategoriesContainerComponent> {

        public static final ResourceLocation ID = ResourceLocation.tryBuild("sdm", "categories_manager");

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public JsonObject serialize(ShopCategoriesContainerComponent component) {
            return new JsonObject();
        }

        @Override
        public ShopCategoriesContainerComponent deserialize(JsonObject json) {
            return new ShopCategoriesContainerComponent();
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ShopCategoriesContainerComponent component) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ShopCategoriesContainerComponent fromNetwork(FriendlyByteBuf buf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ShopCategoriesContainerComponent createDefault() {
            return new ShopCategoriesContainerComponent();
        }

        @Override
        public boolean showInEditor() {
            return false;
        }
    }
}
