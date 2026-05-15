package dev.sixik.sdmshop2.libs.sdmeconomy.network.packets;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.sixik.sdmshop2.libs.sdmeconomy.SDMEconomyCurrencyRegistry;
import dev.sixik.sdmshop2.libs.sdmeconomy.SDMEconomyService;
import dev.sixik.sdmshop2.libs.sdmeconomy.SDMEconomyServiceClient;
import dev.sixik.sdmshop2.libs.sdmeconomy.network.SDMEconomyNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class SendDynamicCurrencyS2C extends BaseS2CMessage {

    private final CompoundTag nbt;

    public SendDynamicCurrencyS2C() {
        nbt = SDMEconomyCurrencyRegistry.serializeCurrencies();
    }

    public SendDynamicCurrencyS2C(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public SendDynamicCurrencyS2C(FriendlyByteBuf buf) {
        nbt = buf.readAnySizeNbt();
    }

    @Override
    public MessageType getType() {
        return SDMEconomyNetwork.SEND_DYNAMIC_CURRENCIES;
    }

    @Override
    public void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeNbt(nbt);
    }

    @Override
    public void handle(NetworkManager.PacketContext packetContext) {
        SDMEconomyServiceClient.CURRENCIES = SDMEconomyCurrencyRegistry.deserializeCurrencies(nbt);
        SDMEconomyService.LOGGER.info("Accepted Custom Currencies from server ! Count: {}", SDMEconomyServiceClient.CURRENCIES.size());
    }
}
