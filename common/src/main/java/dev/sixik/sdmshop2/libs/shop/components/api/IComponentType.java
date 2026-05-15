package dev.sixik.sdmshop2.libs.shop.components.api;

import com.google.gson.JsonObject;
import dev.sixik.sdmshop2.libs.sdmeconomy.icons.CurrencyIcon;
import dev.sixik.sdmshop2.libs.shop.SDMShopConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Интерфейс, определяющий метаданные и методы сериализации для типа компонента.
 * Каждый тип компонента должен реализовать этот интерфейс для поддержки сохранения в JSON и передачи по сети.
 *
 * @param <T> Тип компонента, с которым работает данный IComponentType
 */
public interface IComponentType<T extends ShopComponent> {

    /**
     * Возвращает уникальный идентификатор типа компонента.
     *
     * @return ResourceLocation идентификатор
     */
    ResourceLocation getId();

    /**
     * Возвращает {@link Component#translatable} для UI ли других целей
     *
     * @return Новый {@link Component#translatable}
     */
    default Component getTranslation() {
        return Component.translatable(getTranslationKey());
    }

    default String getTranslationKey() {
        final ResourceLocation id = getId();
        String out = "component." + id.getNamespace();
        return out + "." + id.getPath().replace("/", ".");
    }

    /**
     * Возвращает {@code ID} группы
     */
    default String getCategory() {
        return SDMShopConstants.getCategory(this);
    }

    default CurrencyIcon getIcon() {
        return CurrencyIcon.ICE;
    }

    /**
     * Сериализует компонент в JSON.
     *
     * @param component Экземпляр компонента для сериализации
     * @return JsonObject с данными компонента
     */
    JsonObject serialize(T component);

    /**
     * Десериализует компонент из JSON.
     *
     * @param json JsonObject с данными компонента
     * @return Новый экземпляр компонента
     */
    T deserialize(JsonObject json);

    /**
     * Записывает данные компонента в сетевой буфер.
     *
     * @param buf       Буфер сетевого пакета
     * @param component Экземпляр компонента
     */
    void toNetwork(FriendlyByteBuf buf, T component);

    /**
     * Считывает данные компонента из сетевого буфера и создает новый экземпляр.
     *
     * @param buf Буфер сетевого пакета
     * @return Новый экземпляр компонента
     */
    T fromNetwork(FriendlyByteBuf buf);

    /**
     * Создает экземпляр компонента с настройками по умолчанию.
     *
     * @return Экземпляр компонента по умолчанию
     */
    T createDefault();

    /**
     * Создаёт экземпляр компонента на основе переданных аргументов
     *
     * @return Экземпляр компонента
     */
    default T createFromBuilder(Object... args) {
        return createDefault();
    }

    /**
     * Должен ли данный компонент отображаться в редакторе
     * @return {@code true} - будет отображаться
     */
    default boolean showInEditor() {
        return true;
    }
}
