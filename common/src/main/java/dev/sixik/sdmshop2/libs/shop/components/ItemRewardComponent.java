package dev.sixik.sdmshop2.libs.shop.components;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.sixik.sdmshop2.libs.sdmeconomy.icons.CurrencyIcon;
import dev.sixik.sdmshop2.libs.sdmeconomy.icons.IconType;
import dev.sixik.sdmshop2.libs.shop.components.api.IComponentType;
import dev.sixik.sdmshop2.libs.shop.components.api.RewardComponent;
import dev.sixik.sdmshop2.libs.shop.components.api.annotation.ComponentConfig;
import dev.sixik.sdmshop2.libs.shop.components.api.annotation.ComponentNumberRange;
import dev.sixik.sdmshop2.utils.ShopItemHelper;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemRewardComponent extends RewardComponent {

    public static final IComponentType<ItemRewardComponent> TYPE = new Type();

    @Getter
    @ComponentConfig(translationKey = "shop.component.reward.item.reward_item")
    private ItemStack rewardItem;

    @Getter
    @ComponentConfig(translationKey = "shop.component.reward.item.amount")
    @ComponentNumberRange(intMin = 1)
    private int amount;

    public ItemRewardComponent() {
        this(ItemStack.EMPTY, 1);
    }

    public ItemRewardComponent(Item item, int amount) {
        this(item.getDefaultInstance(), amount);
    }

    public ItemRewardComponent(ItemStack rewardItem, int amount) {
        this.rewardItem = rewardItem.getCount() > 1 ? rewardItem.copyWithCount(1) : rewardItem;
        this.amount = Math.max(1, amount);
    }

    @Override
    public void reward(ServerPlayer player, int inAmount) {
        ShopItemHelper.giveItems(player, rewardItem, (long) amount * inAmount);
    }

    @Override
    public IComponentType<?> getType() {
        return TYPE;
    }

    private static class Type implements IComponentType<ItemRewardComponent> {

        private static final ResourceLocation ID = ResourceLocation.tryBuild("sdm", "reward_item");

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public JsonObject serialize(ItemRewardComponent component) {
            JsonObject json = new JsonObject();

            final ItemStack item = component.rewardItem;
            json.addProperty("item", BuiltInRegistries.ITEM.getKey(item.getItem()).toString());

            if(item.getTag() != null) {
                json.addProperty("nbt", item.getTag().toString());
            }

            if(component.amount > 1)
                json.addProperty("amount", component.amount);

            return json;
        }

        @Override
        public ItemRewardComponent deserialize(JsonObject json) {
            if(!json.has("item"))
                throw new NullPointerException("Param with id 'item' not exists!");

            String itemIdStr = json.get("item").getAsString();
            ResourceLocation itemId = ResourceLocation.tryParse(itemIdStr);

            Item item = BuiltInRegistries.ITEM.get(itemId);

            if (item == null || item == Items.AIR) {
                throw new IllegalArgumentException("Item not found: " + itemIdStr);
            }

            CompoundTag nbt = null;
            if (json.has("nbt")) {
                String nbtString = json.get("nbt").getAsString();
                try {
                    nbt = TagParser.parseTag(nbtString);
                } catch (Exception e) {
                    throw new JsonSyntaxException("Invalid NBT on Item Reward");
                }
            }


            ItemStack itemStack = item.getDefaultInstance();

            if(nbt != null)
                itemStack.setTag(nbt);

            final int amount = json.has("amount") ?
                    json.get("amount").getAsInt() : 1;

            return new ItemRewardComponent(itemStack, amount);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ItemRewardComponent component) {
            buf.writeItem(component.rewardItem);
            buf.writeVarInt(component.amount);
        }

        @Override
        public ItemRewardComponent fromNetwork(FriendlyByteBuf buf) {
            return new ItemRewardComponent(buf.readItem(), buf.readVarInt());
        }

        @Override
        public ItemRewardComponent createDefault() {
            return new ItemRewardComponent();
        }

        @Override
        public ItemRewardComponent createFromBuilder(Object... args) {
            if(args.length != 2)
                throw new IllegalArgumentException("ItemRewardComponent.createFromBuilder() takes 2 arguments (ItemStack, int)");

            return new ItemRewardComponent((ItemStack) args[0], (int) args[1]);
        }

        @Override
        public CurrencyIcon getIcon() {
           return new CurrencyIcon(IconType.ITEM, Items.DIAMOND);
        }
    }
}
