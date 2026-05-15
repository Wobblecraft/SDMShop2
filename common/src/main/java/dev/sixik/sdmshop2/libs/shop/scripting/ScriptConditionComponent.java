package dev.sixik.sdmshop2.libs.shop.scripting;

import com.google.gson.JsonObject;
import dev.sixik.sdmshop2.libs.sdmeconomy.icons.CurrencyIcon;
import dev.sixik.sdmshop2.libs.sdmeconomy.icons.IconType;
import dev.sixik.sdmshop2.libs.shop.components.api.ConditionComponent;
import dev.sixik.sdmshop2.libs.shop.components.api.IComponentType;
import dev.sixik.sdmshop2.libs.shop.components.api.annotation.ComponentConfig;
import dev.sixik.sdmshop2.libs.shop.components.api.annotation.ComponentNumberRange;
import dev.sixik.sdmshop2.libs.shop.scripting.events.ShopScriptEvents;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Компонент условия, который выполняет проверку через скриптовое событие.
 */
public class ScriptConditionComponent extends ConditionComponent {

    /**
     * Тип компонента для регистрации в системе магазинов.
     */
    public static final IComponentType<ScriptConditionComponent> TYPE = new Type();

    /**
     * Идентификатор скрипта, который будет вызван для проверки условия.
     */
    @Getter
    @Setter
    @ComponentConfig(translationKey = "shop.component.condition.script.scrip_id")
    private String scripId = "";

    /**
     * Конструктор по умолчанию.
     */
    public ScriptConditionComponent() { }

    /**
     * Конструктор с указанием идентификатора скрипта.
     *
     * @param scripId Идентификатор скрипта.
     */
    public ScriptConditionComponent(String scripId) {
        this.scripId = scripId;
    }

    /**
     * Проверяет, выполнено ли условие для игрока.
     * Вызывает скриптовое событие {@link ShopScriptEvents#SCRIP_CONDITION_EVENT}.
     *
     * @param player Игрок, для которого проверяется условие.
     * @return true, если условие выполнено, иначе false.
     */
    @Override
    public boolean isChecked(Player player) {
        return ShopScriptEvents.SCRIP_CONDITION_EVENT.invoker().invoke(player, this, scripId);
    }

    /**
     * Возвращает тип компонента.
     *
     * @return Тип компонента.
     */
    @Override
    public IComponentType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean verifiedOnClient() {
        return false;
    }

    /**
     * Класс для регистрации типа компонента в системе магазинов.
     */
    private static class Type implements IComponentType<ScriptConditionComponent> {
        private static final ResourceLocation ID = ResourceLocation.tryBuild("sdm", "condition_script");

        /**
         * Возвращает уникальный идентификатор типа компонента.
         *
         * @return ResourceLocation идентификатор.
         */
        @Override
        public ResourceLocation getId() {
            return ID;
        }

        /**
         * Сериализует компонент в JSON объект.
         *
         * @param component Компонент для сериализации.
         * @return JsonObject с данными компонента.
         */
        @Override
        public JsonObject serialize(ScriptConditionComponent component) {
            JsonObject json = new JsonObject();
            json.addProperty("script_id", component.scripId);
            return json;
        }

        /**
         * Десериализует компонент из JSON объекта.
         *
         * @param json JsonObject с данными компонента.
         * @return Новый экземпляр ScriptConditionComponent.
         */
        @Override
        public ScriptConditionComponent deserialize(JsonObject json) {
            return new ScriptConditionComponent(json.get("script_id").getAsString());
        }

        /**
         * Записывает данные компонента в сетевой буфер.
         *
         * @throws UnsupportedOperationException всегда.
         */
        @Override
        public void toNetwork(FriendlyByteBuf buf, ScriptConditionComponent component) {
            buf.writeUtf(component.scripId);
        }

        /**
         * Читает данные компонента из сетевого буфера.
         *
         * @throws UnsupportedOperationException всегда.
         */
        @Override
        public ScriptConditionComponent fromNetwork(FriendlyByteBuf buf) {
            return new ScriptConditionComponent(buf.readUtf());
        }

        /**
         * Создает экземпляр компонента по умолчанию.
         *
         * @return Новый экземпляр ScriptConditionComponent.
         */
        @Override
        public ScriptConditionComponent createDefault() {
            return new ScriptConditionComponent();
        }

        @Override
        public CurrencyIcon getIcon() {
            return new CurrencyIcon(IconType.ITEM, Items.REPEATING_COMMAND_BLOCK);
        }
    }
}
