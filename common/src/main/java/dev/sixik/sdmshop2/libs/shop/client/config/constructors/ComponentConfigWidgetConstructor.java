package dev.sixik.sdmshop2.libs.shop.client.config.constructors;

import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.gui.widget.layout.Layout;
import com.lowdragmc.lowdraglib.utils.Size;
import dev.sixik.sdmshop2.SDMShop2;
import dev.sixik.sdmshop2.libs.shop.base.ShopEntity;
import dev.sixik.sdmshop2.libs.shop.base.ShopOffer;
import dev.sixik.sdmshop2.libs.shop.client.SDMShopClient;
import dev.sixik.sdmshop2.libs.shop.client.config.ComponentCollapsedGroupWidget;
import dev.sixik.sdmshop2.libs.shop.client.config.ComponentConfigurationWidget;
import dev.sixik.sdmshop2.libs.shop.client.screens.widgets.CollapsedGroupWidget;
import dev.sixik.sdmshop2.libs.shop.client.screens.widgets.ExternTextFieldWidget;
import dev.sixik.sdmshop2.libs.shop.client.screens.widgets.SDMBlockSelectorWidget;
import dev.sixik.sdmshop2.libs.shop.client.screens.widgets.SDMItemStackSelectorWidget;
import dev.sixik.sdmshop2.libs.shop.components.api.ShopComponent;
import dev.sixik.sdmshop2.libs.shop.components.api.annotation.ComponentNumberRange;
import dev.sixik.sdmshop2.libs.shop.components.api.annotation.ComponentStringRegex;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

public class ComponentConfigWidgetConstructor {

    private static int DEFAULT_W = 60;
    private static int DEFAULT_H = 15;

    public static void createShopOfferWidget(WidgetGroup root, ShopEntity offer, int w) {
        final var components = offer.getComponents();

        for (int i = 0; i < components.size(); i++) {
            ShopComponent component = components.get(i);

            CollapsedGroupWidget widget = new ComponentCollapsedGroupWidget(component, offer, w);
            widget.useTabulation();
            widget.addWidget(new ComponentConfigurationWidget(component));
            root.addWidget(widget);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Widget createWidget(ShopComponent targetComponent, ComponentConfigAccess.CachedField cachedField) {
        Class<?> type = cachedField.type();
        
        if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            return createCollectionWidget(targetComponent, cachedField);
        }

        Widget someWidget = createField(targetComponent, cachedField);
        if(someWidget != null) return someWidget;

        someWidget = createSwitch(targetComponent, cachedField);
        if(someWidget != null) return someWidget;

        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();

            SelectorWidget widget = new SelectorWidget();
            List<String> options = Arrays.stream(constants)
                    .map(obj -> ((Enum<?>) obj).name())
                    .toList();
            widget.setCandidates(options);

            try {
                Object currentValue = cachedField.getter().invoke(targetComponent);
                if (currentValue != null) {
                    widget.setValue(((Enum<?>) currentValue).name());
                }
            } catch (Throwable e) {
                SDMShop2.LOGGER.error("Failed to get Enum value", e);
            }

            widget.setOnChanged(selectedString -> {
                try {
                    Enum<?> newValue = Enum.valueOf((Class<Enum>) type, selectedString);
                    cachedField.setter().invoke(targetComponent, newValue);
                    invokeUpdate(targetComponent);
                } catch (IllegalArgumentException e) {
                    SDMShop2.LOGGER.error("Unknown Enum value: {}", selectedString);
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to set Enum value", e);
                }
            });

            return widget;
        }

        if(type == ItemStack.class) {
            SDMItemStackSelectorWidget selectorWidget = new SDMItemStackSelectorWidget(0, 0, DEFAULT_W + 22, false);
            try {
                Object val = cachedField.getter().invoke(targetComponent);
                selectorWidget.setItemStack(val != null ? (ItemStack) val : ItemStack.EMPTY);
            } catch (Throwable e) {
                SDMShop2.LOGGER.error("Failed to get ItemStack value", e);
            }
            selectorWidget.setOnItemStackUpdate(itemStack -> {
                try {
                    cachedField.setter().invoke(targetComponent, itemStack);
                    invokeUpdate(targetComponent);
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to set ItemStack value", e);
                }
            });
            return selectorWidget;
        } else if(type == Item.class) {
            SDMItemStackSelectorWidget selectorWidget = new SDMItemStackSelectorWidget(0, 0, DEFAULT_W + 22, false);
            try {
                Object val = cachedField.getter().invoke(targetComponent);
                selectorWidget.setItemStack(val != null ? ((Item) val).getDefaultInstance() : ItemStack.EMPTY);
            } catch (Throwable e) {
                SDMShop2.LOGGER.error("Failed to get Item value", e);
            }
            selectorWidget.setOnItemStackUpdate(itemStack -> {
                try {
                    cachedField.setter().invoke(targetComponent, itemStack.getItem());
                    invokeUpdate(targetComponent);
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to set Item value", e);
                }
            });
            return selectorWidget;
        }

        if(type == Block.class) {
            SDMBlockSelectorWidget widget = new SDMBlockSelectorWidget(0, 0, DEFAULT_W + 22, false);
            widget.setOnBlockStateUpdate(((blockState) -> {
                try {
                    cachedField.setter().invoke(targetComponent, blockState.getBlock());
                    invokeUpdate(targetComponent);
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to set Block value", e);
                }
            }));

            try {
                Object val = cachedField.getter().invoke(targetComponent);
                widget.setBlock(val != null ? ((Block) val).defaultBlockState() : Blocks.AIR.defaultBlockState());
            } catch (Throwable e) {
                SDMShop2.LOGGER.error("Failed to get Block value", e);
            }

            return widget;
        } else if(type == BlockState.class) {
            SDMBlockSelectorWidget widget = new SDMBlockSelectorWidget(0, 0, DEFAULT_W + 22, false);
            widget.setOnBlockStateUpdate(((blockState) -> {
                try {
                    cachedField.setter().invoke(targetComponent, blockState);
                    invokeUpdate(targetComponent);
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to set BlockState value", e);
                }
            }));

            try {
                Object val = cachedField.getter().invoke(targetComponent);
                widget.setBlock(val != null ? ((BlockState) val) : Blocks.AIR.defaultBlockState());
            } catch (Throwable e) {
                SDMShop2.LOGGER.error("Failed to get BlockState value", e);
            }

            return widget;
        }

        return null;
    }

    private static Widget createCollectionWidget(ShopComponent targetComponent, ComponentConfigAccess.CachedField cachedField) {
        Class<?> collectionType = cachedField.type();
        Class<?> innerType = collectionType.isArray() ? collectionType.getComponentType() : cachedField.innerType();

        if (innerType == null) return null;

        WidgetGroup listContainer = new WidgetGroup(0, 0, 85, 0) {
            @Override
            public void setSize(Size size) {
                super.setSize(size);
                for (Widget w : widgets) {
                    w.setSizeWidth(size.width);
                    if (w instanceof WidgetGroup row && row.widgets.size() >= 2) {
                        Widget editor = row.widgets.get(0);
                        Widget btn = row.widgets.get(1);

                        btn.setSelfPosition(size.width - 15, 0);
                        /*
                            ath.max защищает от отрицательной ширины и крашей OpenGL
                         */
                        editor.setSizeWidth(Math.max(1, size.width - 17));
                    }
                }
            }

            /*
                Блокируем схлопывание ширины в 0 при clearAllWidgets()
             */
            @Override
            protected Size computeDynamicSize() {
                Size size = super.computeDynamicSize();
                return new Size(getSizeWidth(), size.height);
            }
        };
        listContainer.setLayout(Layout.VERTICAL_LEFT);
        listContainer.setLayoutPadding(2);
        listContainer.setDynamicSized(true);

        class UIBuilder {
            void rebuild() {
                /*
                    Запоминаем ширину ДО очистки списка
                 */
                int currentWidth = listContainer.getSizeWidth();
                if (currentWidth < 10) currentWidth = 85; // Fallback для самого первого рендера

                listContainer.clearAllWidgets();
                List<Object> currentList = new ArrayList<>();
                Object existingCollection = null;

                try {
                    existingCollection = cachedField.getter().invoke(targetComponent);
                    if (existingCollection != null) {
                        if (collectionType.isArray()) {
                            int length = java.lang.reflect.Array.getLength(existingCollection);
                            for (int i = 0; i < length; i++) currentList.add(java.lang.reflect.Array.get(existingCollection, i));
                        } else if (existingCollection instanceof Collection<?> coll) {
                            currentList.addAll(coll);
                        }
                    }
                } catch (Throwable ignored) {}

                final Object existingRef = existingCollection;

                for (int i = 0; i < currentList.size(); i++) {
                    final int index = i;
                    Object item = currentList.get(i);

                    WidgetGroup row = new WidgetGroup(0, 0, currentWidth, 15);
                    row.setLayout(Layout.NONE);

                    Widget editor = createListElementEditor(innerType, item, cachedField, newValue -> {
                        currentList.set(index, newValue);
                        saveCollection(targetComponent, cachedField, currentList, collectionType, innerType, existingRef);
                    });

                    ButtonWidget removeBtn = new ButtonWidget(currentWidth - 15, 0, 15, 15, new TextTexture("§c-"), btn -> {
                        currentList.remove(index);
                        saveCollection(targetComponent, cachedField, currentList, collectionType, innerType, existingRef);
                        rebuild();
                    });

                    if (editor != null) {
                        editor.setSelfPosition(0, 0);
                        editor.setSizeHeight(15);
                        editor.setSizeWidth(Math.max(1, currentWidth - 17));
                        row.addWidget(editor);
                    }
                    row.addWidget(removeBtn);
                    listContainer.addWidget(row);
                }

                ButtonWidget addBtn = new ButtonWidget(0, 0, currentWidth, 15, new TextTexture(() -> I18n.get("client.shop.component.editor.arrays.button.add_element")), btn -> {
                    currentList.add(getDefaultValue(innerType));
                    saveCollection(targetComponent, cachedField, currentList, collectionType, innerType, existingRef);
                    rebuild();
                });

                listContainer.addWidget(addBtn);

                /*
                    Принудительно вызываем перерасчет Layout для новых виджетов
                 */
                listContainer.setSizeWidth(currentWidth);
            }
        }

        new UIBuilder().rebuild();
        return listContainer;
    }

    /**
     * Конвертирует наш временный List обратно в нужный тип (Array, Set, List) и сохраняет в компонент.
     */
    @SuppressWarnings("unchecked")
    private static void saveCollection(ShopComponent targetComponent, ComponentConfigAccess.CachedField cachedField, List<Object> list, Class<?> collType, Class<?> innerType, Object existingRef) {
        try {
            if (collType.isArray()) {
                /*
                    Работа с сырыми массивами (Type[])
                 */
                Object array = Array.newInstance(innerType, list.size());
                for (int i = 0; i < list.size(); i++) {
                    Array.set(array, i, list.get(i));
                }
                cachedField.setter().invoke(targetComponent, array);

            } else if (existingRef instanceof Collection) {
                /*
                    Мы просто очищаем её и заливаем новые данные
                 */
                Collection<Object> coll = (Collection<Object>) existingRef;
                coll.clear();
                coll.addAll(list);
                cachedField.setter().invoke(targetComponent, coll);

            } else {
                /*
                     Если поле изначально было null
                 */
                Collection<Object> newColl;
                if (Set.class.isAssignableFrom(collType)) {
                    newColl = new java.util.LinkedHashSet<>(list);
                } else if (collType.getName().contains("fastutil")) {
                    /*
                        Если это тип из FastUtil, создаем ObjectArrayList
                     */
                    newColl = new ObjectArrayList<>(list);
                } else {
                    newColl = new ArrayList<>(list);
                }
                cachedField.setter().invoke(targetComponent, newColl);
            }
            invokeUpdate(targetComponent);
        } catch (Throwable e) {
            SDMShop2.LOGGER.error("Failed to save collection", e);
        }
    }

    /**
     * Возвращает безопасное дефолтное значение при нажатии кнопки "Добавить".
     */
    private static Object getDefaultValue(Class<?> type) {
        if (type == String.class) return "";
        if (type == int.class || type == Integer.class) return 0;
        if (type == long.class || type == Long.class) return 0L;
        if (type == double.class || type == Double.class) return 0.0;
        if (type == float.class || type == Float.class) return 0.0f;
        if (type == boolean.class || type == Boolean.class) return false;
        if (type == UUID.class) return UUID.randomUUID();
        if (type == ResourceLocation.class) return new ResourceLocation("minecraft", "air");
        if (type.isEnum()) return type.getEnumConstants()[0];
        if (type == ItemStack.class) return ItemStack.EMPTY;
        return null;
    }

    /**
     * Создает изолированный виджет для редактирования конкретного значения из коллекции.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Widget createListElementEditor(Class<?> type, Object value, ComponentConfigAccess.CachedField cachedField, java.util.function.Consumer<Object> onChange) {
        if (type == String.class || type == int.class || type == Integer.class || type == float.class || type == Float.class || type == double.class || type == Double.class || type == ResourceLocation.class) {
            ExternTextFieldWidget widget = new ExternTextFieldWidget();

            final ComponentNumberRange numberRange = cachedField.numberRange();

            if (type == int.class || type == Integer.class) {
                int min = numberRange != null ? numberRange.intMin() : Integer.MIN_VALUE;
                int max = numberRange != null ? numberRange.intMax() : Integer.MAX_VALUE;
                widget.setNumbersOnly(min, max);
            }
            else if (type == float.class || type == Float.class) {
                float min = numberRange != null ? numberRange.floatMin() : -Float.MAX_VALUE;
                float max = numberRange != null ? numberRange.floatMax() : Float.MAX_VALUE;
                widget.setNumbersOnly(min, max);
            }
            else if (type == double.class || type == Double.class) {
                double min = numberRange != null ? numberRange.doubleMin() : -Double.MAX_VALUE;
                double max = numberRange != null ? numberRange.doubleMax() : Double.MAX_VALUE;
                widget.setNumbersOnly(min, max);
            }
            else if(type == ResourceLocation.class) {
                widget.setResourceLocationOnly();
            }

            widget.setCurrentString(value != null ? String.valueOf(value) : "");

            widget.setTextResponder(text -> {
                if (text == null || text.isEmpty() || text.equals("-") || text.equals(".")) return;
                try {
                    if (type == int.class || type == Integer.class) onChange.accept(Integer.parseInt(text));
                    else if (type == float.class || type == Float.class) onChange.accept(Float.parseFloat(text));
                    else if (type == double.class || type == Double.class) onChange.accept(Double.parseDouble(text));
                    else if (type == ResourceLocation.class) onChange.accept(ResourceLocation.tryParse(text));
                    else onChange.accept(text); // String
                } catch (Exception ignored) {}
            });
            return widget;
        }

        else if (type.isEnum()) {
            SelectorWidget widget = new SelectorWidget();
            List<String> options = Arrays.stream(type.getEnumConstants()).map(obj -> ((Enum<?>) obj).name()).toList();
            widget.setCandidates(options);
            if (value != null) widget.setValue(((Enum<?>) value).name());

            widget.setOnChanged(selectedString -> {
                try {
                    onChange.accept(Enum.valueOf((Class<Enum>) type, selectedString));
                } catch (Exception ignored) {}
            });
            return widget;
        }

        else if (type == boolean.class || type == Boolean.class) {
            SwitchWidget widget = new SwitchWidget();
            widget.setPressed(Boolean.TRUE.equals(value));
            widget.setOnPressCallback((s1, s2) -> onChange.accept(s2));
            return widget;
        }

        return null;
    }

    private static @Nullable SwitchWidget createSwitch(ShopComponent targetComponent, ComponentConfigAccess.CachedField cachedField) {
        SwitchWidget widget = new SwitchWidget();
        Class<?> type = cachedField.type();
        if(type == boolean.class || type == Boolean.class) {
            try {
                widget.setPressed(cachedField.getter().invoke(targetComponent) == Boolean.TRUE);
            } catch (Throwable e) {
                SDMShop2.LOGGER.error("Failed to get boolean value", e);
                widget.setPressed(false);
            }
            widget.setOnPressCallback((s1, s2) -> {
                try {
                    cachedField.setter().invoke(targetComponent, s2);
                    invokeUpdate(targetComponent);
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to toggle boolean value", e);
                }
            });
        } else
            return null;

        return widget;
    }

    private static @Nullable ExternTextFieldWidget createField(ShopComponent targetComponent, ComponentConfigAccess.CachedField cachedField) {
        ExternTextFieldWidget widget = new ExternTextFieldWidget();
        Class<?> type = cachedField.type();
        final ComponentNumberRange numberRange = cachedField.numberRange();

        if(type == ResourceLocation.class) {
            widget.setResourceLocationOnly();
            widget.setTextSupplier(() -> {
                try {
                    Object val = cachedField.getter().invoke(targetComponent);
                    return val != null ? val.toString() : "";
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to get ResourceLocation value", e);
                    return "";
                }
            });
            widget.setTextResponder(text -> {
                if (text == null || text.isEmpty()) return;

                ResourceLocation loc = text.contains(":")
                        ? ResourceLocation.tryParse(text)
                        : ResourceLocation.tryBuild("minecraft", text);

                if (loc != null) {
                    try {
                        cachedField.setter().invoke(targetComponent, loc);
                        invokeUpdate(targetComponent);
                    } catch (Throwable e) {
                        SDMShop2.LOGGER.error("Failed to set ResourceLocation value", e);
                    }
                }
            });
        } else if(type == UUID.class) {
            widget.setUuidOnly();
            widget.setTextSupplier(() -> {
                try {
                    Object val = cachedField.getter().invoke(targetComponent);
                    return val != null ? val.toString() : "";
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to get UUID value", e);
                    return "";
                }
            });
            widget.setTextResponder(text -> {
                try {
                    cachedField.setter().invoke(targetComponent, UUID.fromString(text));
                    invokeUpdate(targetComponent);
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to set UUID value", e);
                }
            });
        } else if (type == String.class) {
            widget.setTextSupplier(() -> {
                try {
                    Object val = cachedField.getter().invoke(targetComponent);
                    return val != null ? (String) val : "";
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to get String value", e);
                    return "";
                }
            });
            widget.setTextResponder(text -> {
                if (cachedField.stringRegex() != null) {
                    ComponentStringRegex regexInfo = cachedField.stringRegex();
                    if (!text.matches(regexInfo.value())) {
                        SDMShop2.LOGGER.warn("Regex validation failed for {}: Expected {} but got '{}' ({})",
                                cachedField.translationKey(), regexInfo.value(), text, regexInfo.errorMessage());
                        return;
                    }
                }

                try {
                    cachedField.setter().invoke(targetComponent, text);
                    invokeUpdate(targetComponent);
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to set String value", e);
                }
            });
        }
        else if (type == int.class || type == Integer.class) {
            var min = numberRange != null ? numberRange.intMin() : Integer.MIN_VALUE;
            var max = numberRange != null ? numberRange.intMax() : Integer.MAX_VALUE;
            widget.setNumbersOnly(min, max);
            widget.setTextSupplier(() -> {
                try {
                    return String.valueOf(cachedField.getter().invoke(targetComponent));
                } catch (Throwable e) {
                    return "0";
                }
            });
            widget.setTextResponder(text -> {
                if (text.isEmpty() || text.equals("-")) return;
                try {
                    cachedField.setter().invoke(targetComponent, Integer.parseInt(text));
                    invokeUpdate(targetComponent);
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to set int value", e);
                }
            });
        }
        else if (type == long.class || type == Long.class) {
            var min = numberRange != null ? numberRange.longMin() : Long.MIN_VALUE;
            var max = numberRange != null ? numberRange.longMax() : Long.MAX_VALUE;
            widget.setNumbersOnly(min, max);
            widget.setTextSupplier(() -> {
                try {
                    return String.valueOf(cachedField.getter().invoke(targetComponent));
                } catch (Throwable e) {
                    return "0";
                }
            });
            widget.setTextResponder(text -> {
                if (text.isEmpty() || text.equals("-")) return;
                try {
                    cachedField.setter().invoke(targetComponent, Long.parseLong(text));
                    invokeUpdate(targetComponent);
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to set long value", e);
                }
            });
        }
        else if (type == double.class || type == Double.class) {
            var min = numberRange != null ? numberRange.doubleMin() : -Double.MAX_VALUE;
            var max = numberRange != null ? numberRange.doubleMax() : Double.MAX_VALUE;
            widget.setNumbersOnly(min, max);
            widget.setTextSupplier(() -> {
                try {
                    return String.valueOf(cachedField.getter().invoke(targetComponent));
                } catch (Throwable e) {
                    return "0.0";
                }
            });
            widget.setTextResponder(text -> {
                if (text.isEmpty() || text.equals("-") || text.equals(".")) return;
                try {
                    cachedField.setter().invoke(targetComponent, Double.parseDouble(text));
                    invokeUpdate(targetComponent);
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to set double value", e);
                }
            });
        }
        else if (type == float.class || type == Float.class) {
            var min = numberRange != null ? numberRange.floatMin() : -Float.MAX_VALUE;
            var max = numberRange != null ? numberRange.floatMax() : Float.MAX_VALUE;
            widget.setNumbersOnly(min, max);
            widget.setTextSupplier(() -> {
                try {
                    return String.valueOf(cachedField.getter().invoke(targetComponent));
                } catch (Throwable e) {
                    return "0.0";
                }
            });
            widget.setTextResponder(text -> {
                if (text.isEmpty() || text.equals("-") || text.equals(".")) return;
                try {
                    cachedField.setter().invoke(targetComponent, Float.parseFloat(text));
                    invokeUpdate(targetComponent);
                } catch (Throwable e) {
                    SDMShop2.LOGGER.error("Failed to set float value", e);
                }
            });
        }
        else {
            return null;
        }

        try {
            widget.setCurrentString(String.valueOf(cachedField.getter().invoke(targetComponent)));
        } catch (Throwable e) {
            widget.setCurrentString("0.0");
        }
        return widget;
    }

    private static void invokeUpdate(ShopComponent targetComponent) {
        SDMShopClient.UPDATE_COMPONENT_EVENT.invoker().onUpdateComponentEvent(targetComponent.getRoot(), targetComponent);
    }
}
