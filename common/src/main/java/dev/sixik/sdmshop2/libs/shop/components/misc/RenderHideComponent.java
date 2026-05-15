package dev.sixik.sdmshop2.libs.shop.components.misc;

import com.google.gson.JsonObject;
import dev.sixik.sdmshop2.libs.shop.components.api.ConditionComponent;
import dev.sixik.sdmshop2.libs.shop.components.api.IComponentType;
import dev.sixik.sdmshop2.libs.shop.components.api.ShopComponent;
import dev.sixik.sdmshop2.libs.shop.components.exceptions.NoSuchComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class RenderHideComponent extends ShopComponent {

    public static final IComponentType<RenderHideComponent> TYPE = new Type();

    @Override
    public IComponentType<?> getType() {
        return TYPE;
    }

    @Override
    public void init() {
        if(!getRoot().hasComponent(ConditionComponent.class))
            throw new NoSuchComponents(getClass(), ConditionComponent.class);
    }

    private static class Type implements IComponentType<RenderHideComponent> {

        private static final ResourceLocation ID = ResourceLocation.tryBuild("sdm", "hide_render");

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public JsonObject serialize(RenderHideComponent component) {
            return new JsonObject();
        }

        @Override
        public RenderHideComponent deserialize(JsonObject json) {
            return new RenderHideComponent();
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RenderHideComponent component) { }

        @Override
        public RenderHideComponent fromNetwork(FriendlyByteBuf buf) {
            return new RenderHideComponent();
        }

        @Override
        public RenderHideComponent createDefault() {
            return new RenderHideComponent();
        }

        @Override
        public boolean showInEditor() {
            return false;
        }
    }
}
