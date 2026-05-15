package dev.sixik.sdmshop2.libs.sdmeconomy;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class SDMEconomyServiceClient extends SDMEconomyService {

    public static Object2ObjectOpenHashMap<ResourceLocation, IExternalCurrency> CURRENCIES = new Object2ObjectOpenHashMap<>();

    public static Object2ObjectOpenHashMap<ResourceLocation, IExternalCurrency> getAllCurrencies() {
        Object2ObjectOpenHashMap<ResourceLocation, IExternalCurrency> map = new Object2ObjectOpenHashMap<>(SDMEconomyCurrencyRegistry.getCurrenciesMap());
        if(SDMEconomyPlatform.server == null)
            map.putAll(SDMEconomyServiceClient.CURRENCIES);
        return map;
    }

    @Getter
    private static final SDMEconomyServiceClient InstanceClient = new SDMEconomyServiceClient();

    @Getter
    protected BankAccount bankAccount;

    public SDMEconomyServiceClient() {
        this.bankAccount = new BankAccount(Minecraft.getInstance().player);
    }

    public SDMEconomyServiceClient(BankAccount account) {
        this.bankAccount = account;
    }

    @Override
    @Deprecated
    public BankAccount getAccount(UUID gameProfileId) {
        return bankAccount;
    }

    @Override
    public void unloadPlayer(UUID gameProfileId) { }

    @Override
    public void saveAllDirty() { }
}
