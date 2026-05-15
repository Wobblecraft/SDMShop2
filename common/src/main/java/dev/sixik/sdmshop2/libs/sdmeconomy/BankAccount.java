package dev.sixik.sdmshop2.libs.sdmeconomy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import dev.sixik.sdmshop2.utils.NbtExtern;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BankAccount {

    /**
     * Владелец Аккаунта. Это всегда ID {@link GameProfile}
     */
    @Getter
    private final UUID gameProfileOwnerId;

    private final Map<ResourceLocation, BigDecimal> balances = new Object2ObjectOpenHashMap<>();

    @Getter
    private boolean dirty = false;

    @Setter
    private Runnable onUpdate = () -> {};

    public BankAccount(Player player) {
        this(player.getGameProfile());
    }

    public BankAccount(GameProfile profile) {
        this(profile.getId());
    }

    public BankAccount(UUID gameProfileId) {
        this.gameProfileOwnerId = gameProfileId;
    }

    /**
     * Возвращает баланс игрока или стандартное значение валюты {@link IStoredCurrency#getDefaultBalance()} если у игрока нет этой валюты
     */
    public BigDecimal getBalance(IStoredCurrency currency) {
        return balances.getOrDefault(currency.getId(), currency.getDefaultBalance());
    }

    /**
     * Устанавливает количество конкретной валюты
     */
    public void setBalance(IStoredCurrency currency, BigDecimal amount) {
        try {
            balances.put(currency.getId(), amount);
        } finally {
            onUpdate.run();
        }
    }

    /**
     * Изменяет количество конкретной валюты
     * @param amount Сколько добавить или убавить
     */
    public void modify(IStoredCurrency currency, BigDecimal amount) {
        try {
            balances.merge(currency.getId(), amount, BigDecimal::add);
        } finally {
            onUpdate.run();
        }
    }

    public boolean hasMoney(ResourceLocation moneyId) {
        return balances.containsKey(moneyId);
    }

    public boolean hasMoney(IStoredCurrency currency) {
        return hasMoney(currency.getId());
    }

    @Nullable
    public BigDecimal removeBalance(IStoredCurrency currency) {
        try {
            return balances.remove(currency.getId());
        } finally {
            onUpdate.run();
        }
    }

    public List<ResourceLocation> getCurrenciesIds() {
        return new ArrayList<>(balances.keySet());
    }

    /**
     * Помечает то что данные изменились и их нужно сохранить
     */
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * Помечает то что данные сохранять не нужно
     */
    public void markClean() {
        this.dirty = false;
    }

    public CompoundTag serializeNbt() {
        final CompoundTag nbt = new CompoundTag();
        final ListTag list = new ListTag();
        balances.forEach((id, val) -> {
            final CompoundTag entry = new CompoundTag();
            entry.putString("id", id.toString());
            entry.putString("val", val.toString());
            list.add(entry);
        });
        nbt.put("balances", list);
        return nbt;
    }

    public void deserializeNbt(final CompoundTag nbt) {
        final ListTag list = NbtExtern.getOrThrow(nbt, "balances");

        balances.clear();

        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);

            balances.put(
                    ResourceLocation.tryParse(entry.getString("id")),
                    new BigDecimal(entry.getString("val"))
            );
        }
    }

    public JsonObject serializeJson() {
        final JsonObject json = new JsonObject();

        json.addProperty("owner", this.gameProfileOwnerId.toString());
        final JsonObject balancesJson = new JsonObject();
        balances.forEach((id, val) -> {
            balancesJson.addProperty(id.toString(), val.toString());
        });

        json.add("balances", balancesJson);

        return json;
    }

    public static BankAccount deserializeJson(JsonObject json) {
        UUID ownerId = UUID.fromString(json.get("owner").getAsString());
        BankAccount account = new BankAccount(ownerId);

        if (json.has("balances")) {
            JsonObject balancesJson = json.getAsJsonObject("balances");

            for (Map.Entry<String, JsonElement> entry : balancesJson.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                if (id == null) continue;

                try {
                    BigDecimal val = new BigDecimal(entry.getValue().getAsString());
                    account.balances.put(id, val);
                } catch (NumberFormatException e) {
                    SDMEconomyService.LOGGER.error("Failed to parse balance for currency {} on account {}", id, ownerId);
                }
            }
        }
        return account;
    }
}
