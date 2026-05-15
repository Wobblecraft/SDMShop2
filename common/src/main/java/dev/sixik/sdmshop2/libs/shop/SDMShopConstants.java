package dev.sixik.sdmshop2.libs.shop;

import dev.sixik.sdmshop2.libs.shop.components.api.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;

public class SDMShopConstants {

    private static Map<Class<? extends IComponentType>, String> CATEGORIES_MAP_CACHE = new Object2ObjectOpenHashMap<>();

    public static String getCategory(IComponentType<?> category) {
        String id = CATEGORIES_MAP_CACHE.get(category.getClass());
        if(id != null) return id;

        final var defaultObj = category.createDefault();
        if(defaultObj instanceof ConditionComponent) {
            id = CONDITION_GROUP;
        }
        else if(defaultObj instanceof CostComponent) {
            id = COST_GROUP;
        }
        else if(defaultObj instanceof PromoComponent) {
            id = PROMO_GROUP;
        }
        else if(defaultObj instanceof PromoEffectComponent) {
            id = PROMO_EFFECT_GROUP;
        }
        else if(defaultObj instanceof RewardComponent) {
            id = REWARD_GROUP;
        }
        else {
            id = MISC_GROUP;
        }

        CATEGORIES_MAP_CACHE.put(category.getClass(), id);
        return id;
    }

    public static final String ALL_GROUP =          "component.sdm.caregory.all";
    public static final String MISC_GROUP =         "component.sdm.caregory.misc";
    public static final String CONDITION_GROUP =    "component.sdm.caregory.condition";
    public static final String REWARD_GROUP =       "component.sdm.caregory.reward";
    public static final String COST_GROUP =         "component.sdm.caregory.cost";
    public static final String PROMO_GROUP =        "component.sdm.caregory.promo";
    public static final String PROMO_EFFECT_GROUP = "component.sdm.caregory.promo_effect";
}
