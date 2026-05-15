package dev.sixik.sdmshop2.libs.shop.components;

import com.google.gson.JsonObject;
import dev.sixik.sdmshop2.libs.sdmeconomy.icons.CurrencyIcon;
import dev.sixik.sdmshop2.libs.sdmeconomy.icons.IconType;
import dev.sixik.sdmshop2.libs.shop.components.api.IComponentType;
import dev.sixik.sdmshop2.libs.shop.components.api.RewardComponent;
import dev.sixik.sdmshop2.libs.shop.components.api.annotation.ComponentConfig;
import lombok.Getter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

public class CommandRewardComponent extends RewardComponent {

    public static final String EMPTY = "Command";
    public static final String DEFAULT_COMMAND = "/time set day";

    public static final IComponentType<CommandRewardComponent> TYPE = new Type();

    @Getter
    @ComponentConfig(translationKey = "shop.component.reward.command.display_name")
    private String displayName;

    @Getter
    @ComponentConfig(translationKey = "shop.component.reward.command.command")
    private String command;

    public CommandRewardComponent() {
        this(DEFAULT_COMMAND, EMPTY);
    }

    public CommandRewardComponent(String command, String displayName) {
        this.command = command;
        this.displayName = displayName;
    }

    @Override
    public void reward(ServerPlayer player, int amount) {
        CommandSourceStack source = player.createCommandSourceStack();
        source.withPermission(2);
        source.withSuppressedOutput();

        String format = formatCommand(command, player);
        for (int i = 0; i < amount; i++) {
            player.getServer().getCommands().performPrefixedCommand(source, format);
        }
    }

    @Override
    public IComponentType<?> getType() {
        return TYPE;
    }

    private static class Type implements IComponentType<CommandRewardComponent> {

        private static final ResourceLocation ID = ResourceLocation.tryBuild("sdm", "reward_command");

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public JsonObject serialize(CommandRewardComponent component) {
            JsonObject json = new JsonObject();

            if(!component.displayName.equals(EMPTY))
                json.addProperty("name", component.displayName);

            if(!component.command.equals(DEFAULT_COMMAND))
                json.addProperty("command", component.command);

           return json;
        }

        @Override
        public CommandRewardComponent deserialize(JsonObject json) {

            String command = DEFAULT_COMMAND;
            String name = EMPTY;

            if(json.has("name"))
                name = json.get("name").getAsString();

            if(json.has("command"))
                command = json.get("command").getAsString();

            return new CommandRewardComponent(command, name);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, CommandRewardComponent component) {
            buf.writeUtf(component.displayName);
        }

        @Override
        public CommandRewardComponent fromNetwork(FriendlyByteBuf buf) {
            return new CommandRewardComponent(DEFAULT_COMMAND, buf.readUtf());
        }

        @Override
        public CommandRewardComponent createDefault() {
            return new CommandRewardComponent();
        }

        @Override
        public CommandRewardComponent createFromBuilder(Object... args) {
            if(args.length != 2)
                throw new IllegalArgumentException("CommandRewardComponent.createFromBuilder() takes 2 arguments (String, String)");

            return new CommandRewardComponent((String) args[0], (String) args[1]);
        }

        @Override
        public CurrencyIcon getIcon() {
            return new CurrencyIcon(IconType.ITEM, Items.COMMAND_BLOCK);
        }
    }

    protected static String formatCommand(String command, ServerPlayer player) {
        String copy = command;

        if(command.contains("{player}"))
            copy = copy.replace("{player}", player.getName().getString());
        return copy;
    }
}
