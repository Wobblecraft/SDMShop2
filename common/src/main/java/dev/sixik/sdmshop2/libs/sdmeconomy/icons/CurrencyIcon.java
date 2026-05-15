package dev.sixik.sdmshop2.libs.sdmeconomy.icons;

import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Holder для хранения данных о инонке
 * @param type Тип (Item, Texture, None)
 * @param icon Иконка в зависимости от типа. Если (None то Null)
 */
public record CurrencyIcon(IconType type, @Nullable Object icon) {

    public static final CurrencyIcon EMPTY = new CurrencyIcon(IconType.NONE, null);

    public static final CurrencyIcon ICE = new CurrencyIcon(IconType.ITEM, Items.ICE);
}
