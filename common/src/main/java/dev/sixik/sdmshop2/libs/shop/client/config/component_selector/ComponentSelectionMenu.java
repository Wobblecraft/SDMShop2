package dev.sixik.sdmshop2.libs.shop.client.config.component_selector;

import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.widget.layout.Align;
import com.lowdragmc.lowdraglib.gui.widget.layout.Layout;
import com.mojang.blaze3d.platform.Window;
import dev.sixik.sdmshop2.SDMShop2;
import dev.sixik.sdmshop2.libs.sdmeconomy.icons.CurrencyIcon;
import dev.sixik.sdmshop2.libs.shop.SDMShopConstants;
import dev.sixik.sdmshop2.libs.shop.client.textures.ColorRectAndBorderTexture;
import dev.sixik.sdmshop2.libs.shop.components.api.IComponentType;
import dev.sixik.sdmshop2.libs.shop.components.api.ShopComponent;
import dev.sixik.sdmshop2.libs.shop.components.api.ShopComponentRegistry;
import dev.sixik.sdmshop2.mixin.minecraft.SimpleTextureAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Consumer;

public class ComponentSelectionMenu {

    private static List<String> cachedCategories;
    private static List<IComponentType<?>> cachedSortedComponents;
    private static String currentCategory = SDMShopConstants.ALL_GROUP;

    public static DialogWidget showComponentSelector(
            final WidgetGroup parent,
            final Consumer<ShopComponent> onSelected
    ) {
        return showComponentSelector(parent, Component.translatable("client.shop.component.editor.component_selector.title"), onSelected);
    }

    public static DialogWidget showComponentSelector(
            final WidgetGroup parent,
            final Component title,
            final Consumer<ShopComponent> onSelected
    ) {
        final Window mcWindow = Minecraft.getInstance().getWindow();

        final int margin = 10;
        final int sw = mcWindow.getGuiScaledWidth();
        final int sh = mcWindow.getGuiScaledHeight();
        final DialogWidget dialog = new DialogWidget(0, 0, sw, sh);
        dialog.setClientSideWidget();
        parent.addWidget(dialog);

        dialog.setClickClose(true);
        dialog.setParentInVisible();

        final int availW = Math.max(1, sw - margin * 2);
        final int availH = Math.max(1, sh - margin * 2);

        final int winWidth = (int) (availW * 0.95f);
        final int winHeight = (int) (availH * 0.95f);

        final WidgetGroup window = new WidgetGroup(0, 0, winWidth, winHeight);
        window.setAlign(Align.CENTER);

        final WidgetGroup titleGroup = new WidgetGroup(0, 0, winWidth, 15);
        titleGroup.setBackground(new GuiTextureGroup(
                ColorPattern.RED.rectTexture().setTopRadius(5f),
                ColorPattern.GRAY.borderTexture(-1).setTopRadius(5f),
                new TextTexture(title.getString()).setWidth(winWidth).setDropShadow(false).setType(TextTexture.TextType.ROLL)
        ));
        window.addWidget(titleGroup);

        final WidgetGroup contentGroup = new WidgetGroup(0, 15, winWidth, winHeight - 15);
        contentGroup.setBackground(new GuiTextureGroup(
                ColorPattern.BLACK.rectTexture().setBottomRadius(5f),
                ColorPattern.GRAY.borderTexture(-1).setBottomRadius(5f)
        ));
        window.addWidget(contentGroup);
        dialog.addWidget(window);

        final int contentWidth = contentGroup.getSizeWidth();
        final int contentHeight = contentGroup.getSizeHeight();

        /*
            Скролл панель
         */
        final DraggableScrollableWidgetGroup scroll = new DraggableScrollableWidgetGroup(5, 30, contentWidth - 10, contentHeight - 55);
        scroll.setYScrollBarWidth(4);
        scroll.setYBarStyle(null, ColorPattern.WHITE.rectTexture().setRadius(2));

        final WidgetGroup gridContainer = new WidgetGroup(0, 0, contentWidth - 15, 0);
        gridContainer.setLayout(Layout.NONE);
        gridContainer.setDynamicSized(true);

        /*
            Класс для создания сетки элементов
         */
        final class GridBuilder {
            void rebuild() {
                gridContainer.clearAllWidgets();

                int tileW = 100;
                int tileH = 60;
                int spacing = 5;
                int columns = Math.max(1, (contentWidth - 15) / (tileW + spacing));

                int index = 0;
                for (IComponentType<?> type : getSortedComponents()) {
                    if(!type.showInEditor()) continue;

                    String typeCategory = getComponentCategory(type);
                    if (!currentCategory.equals(SDMShopConstants.ALL_GROUP) && !currentCategory.equals(typeCategory)) {
                        continue;
                    }

                    int col = index % columns;
                    int row = index / columns;

                    int x = col * (tileW + spacing);
                    int y = row * (tileH + spacing);

                    Widget tile = createTileWidget(x, y, tileW, tileH, type, () -> {
                        dialog.close();
                        if (onSelected != null) onSelected.accept(type.createDefault());
                    });

                    gridContainer.addWidget(tile);
                    index++;
                }
                gridContainer.setSizeWidth(contentWidth - 15);
            }
        }

        final GridBuilder builder = new GridBuilder();

        /*
            Добавляем сетку в скролл, а скролл в окно
         */
        scroll.addWidget(gridContainer);
        contentGroup.addWidget(scroll);

        /*
            Кнопка "Отмена" (добавляем до селектора!)
         */
        DialogWidget.createButton(contentGroup, (contentWidth - 60) / 2, contentHeight - 20, 60, 15, "ldlib.gui.tips.cancel", () -> {
            dialog.close();
            if (onSelected != null) onSelected.accept(null);
        });

        /*
            Выпадающий список категортий (Обязательно добавляем посследним!)
         */
        final List<String> categories = getCategories();

        // width: contentWidth - 55 (5 слева, 5 справа от кнопки, 40 на саму кнопку, 5 отступ справа = 55)
        final SelectorWidget categorySelector = new SelectorWidget(5, 5, contentWidth - 55, 20, List.of(), -1);
        categorySelector.setButtonBackground(ColorPattern.T_GRAY.rectTexture());
        categorySelector.setHoverTexture(ColorPattern.T_LIGHT_GRAY.rectTexture());
        categorySelector.setCandidates(categories);

        /*
            Устанавливаем текущую категорию (чтобы окно запоминало выбор при переоткрытии)
         */
        if (categories.contains(currentCategory)) {
            categorySelector.setValue(currentCategory);
        }

        categorySelector.setOnChanged(selectedString -> {
            currentCategory = selectedString;
            builder.rebuild();
        });

        /*
            Кнопка Wiki
         */
        final ButtonWidget wikiButton = new ButtonWidget(contentWidth - 45, 5, 40, 20, null, btn -> {
            String url = I18n.get("client.shop.component.editor.button.wiki").trim();

            if (url.contains("http")) {
                url = url.substring(url.indexOf("http"));
            }

            try {
                Util.getPlatform().openUri(new java.net.URI(url));
            } catch (Exception e) {
                SDMShop2.LOGGER.error("Malformed Wiki URL: {}", url);
            }
        });
        wikiButton.setBackground(ColorPattern.T_GRAY.rectTexture(), new TextTexture("Wiki"));
        wikiButton.setHoverTexture(ColorPattern.T_LIGHT_GRAY.rectTexture());
        wikiButton.setHoverTooltips(Component.translatable("client.shop.component.editor.button.wiki.tooltip"));

        /*
            Это гарантирует, что выпадающее меню отрисуется поверх плиток и кнопок!
         */
        contentGroup.addWidget(wikiButton);
        contentGroup.addWidget(categorySelector);

        /*
            Строим сетку в первый раз
         */
        builder.rebuild();

        return dialog;
    }

    private static String getComponentCategory(final IComponentType<?> type) {
        return type.getCategory();
    }

    private static List<IComponentType<?>> getSortedComponents() {
        if (cachedSortedComponents != null) {
            return cachedSortedComponents;
        }

        List<IComponentType<?>> list = new ObjectArrayList<>(ShopComponentRegistry.getTypes().values());

        /*
            Сортируем Категория -> Имя (По алфавиту без учета регистра)
         */
        list.sort((c1, c2) -> {
            int categoryCompare = c1.getCategory().compareToIgnoreCase(c2.getCategory());
            if (categoryCompare != 0) {
                return categoryCompare;
            }

            // Если категории равны, сортируем по локализованному имени
            String name1 = c1.getTranslation().getString();
            String name2 = c2.getTranslation().getString();
            return name1.compareToIgnoreCase(name2);
        });

        cachedSortedComponents = list;
        return list;
    }

    private static List<String> getCategories() {
        if (cachedCategories != null)
            return cachedCategories;

        Set<String> categories = new ObjectOpenHashSet<>();
        for (IComponentType<?> value : ShopComponentRegistry.getTypes().values()) {
            categories.add(value.getCategory());
        }

        List<String> sortedCategories = new ArrayList<>(categories);
        Collections.sort(sortedCategories);

        ObjectArrayList<String> result = new ObjectArrayList<>(sortedCategories.size() + 1);
        result.add(SDMShopConstants.ALL_GROUP);
        result.addAll(sortedCategories);

        cachedCategories = result;
        return result;
    }

    private static Widget createTileWidget(
            final int x, final int y, final int w, final int h,
            final IComponentType<?> type,
            final Runnable onClick
    ) {
        final WidgetGroup tile = new WidgetGroup(x, y, w, h) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {

                if(isMouseOverElement(mouseX, mouseY)) {

                    if(button == 0) {
                        Widget parentScroll = this.getParent().getParent();
                        if (parentScroll != null && !parentScroll.isMouseOverElement(mouseX, mouseY)) {
                            return false;
                        }

                        Widget.playButtonClickSound();
                        onClick.run();
                        return true;
                    }

                    if (button == 1) {
                        openContextMenu(this, (int) mouseX, (int) mouseY, type);
                        Widget.playButtonClickSound();
                        return true;
                    }
                }

                return super.mouseClicked(mouseX, mouseY, button);
            }
        };

        tile.setBackground(new ColorRectAndBorderTexture(
                ColorPattern.GRAY.color,
                ColorPattern.T_GRAY.color,
                1f
        ).setRadius(4f));
        tile.setHoverTexture(
                new ColorRectAndBorderTexture(
                        ColorPattern.GRAY.color,
                        ColorPattern.T_LIGHT_GRAY.color,
                        1f
                ).setRadius(4f)
        );

        final String tooltipKey = type.getTranslationKey() + ".tooltip";
        final boolean hasTooltip = I18n.exists(tooltipKey);

        Component description = null;

        if (hasTooltip) {
            description = Component.translatable(tooltipKey);
            tile.setHoverTooltips(description);
        }


        final Font font = Minecraft.getInstance().font;

        /*
            Иконка
         */
        final CurrencyIcon componentIcon = type.getIcon();
        final Object componentIconObject = componentIcon.icon();
        final IGuiTexture iconTexture = switch (componentIcon.type()) {
            case ITEM -> {
                if (componentIconObject instanceof Item item)
                    yield new ItemStackTexture(item);
                else if (componentIconObject instanceof ItemStack itemStack)
                    yield new ItemStackTexture(itemStack);
                else
                    yield new ItemStackTexture((ItemStack) CurrencyIcon.ICE.icon());
            }
            case TEXTURE -> {
                if (componentIconObject instanceof ResourceLocation id)
                    yield new ResourceTexture(id);
                else if (componentIconObject instanceof SimpleTexture simpleTexture)
                    yield new ResourceTexture(((SimpleTextureAccessor) simpleTexture).getLocation());
                else if (componentIconObject instanceof String id)
                    yield new ResourceTexture(ResourceLocation.tryParse(id));
                else
                    yield new ItemStackTexture((ItemStack) CurrencyIcon.ICE.icon());
            }
            case NONE -> new ItemStackTexture((ItemStack) CurrencyIcon.ICE.icon());
        };

        final ImageWidget icon = new ImageWidget((w - 24) / 2, 5, 24, 24, iconTexture);
        if (hasTooltip)
            icon.setHoverTooltips(description);
        tile.addWidget(icon);

        /*
            Имя
         */
        final Component name = type.getTranslation();
        final TextTexture nameTexture = new TextTexture(name::getString).setWidth(w - 4).setType(TextTexture.TextType.ROLL);
        final ImageWidget nameWidget = new ImageWidget(2, 35, w - 4, font.lineHeight, nameTexture);
        if (hasTooltip)
            nameWidget.setHoverTooltips(description);
        tile.addWidget(nameWidget);

        return tile;
    }

    private static void openContextMenu(Widget widget, int mouseX, int mouseY, IComponentType<?> type) {
        if (widget.getGui() == null) return;

        Widget root = widget;
        while (root.getParent() != null) {
            root = root.getParent();
        }

        if (!(root instanceof WidgetGroup mainGroup)) return;

        WidgetGroup contextMenu = new WidgetGroup(mouseX, mouseY, 120, 0) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                boolean handled = super.mouseClicked(mouseX, mouseY, button);
                /*
                    Если кликнули по кнопке в меню - handled будет true.
                    Если кликнули мимо кнопок, но внутри меню - мы тоже не закрываемся.
                 */
                if (!isMouseOverElement(mouseX, mouseY)) {
                    mainGroup.removeWidget(this);
                }
                return handled;
            }

            @Override
            public void onFocusChanged(Widget lastFocus, Widget focus) {
                /*
                    Если фокус ушел с меню и его детей - удаляем меню
                 */
                if (!isFocus()) {
                    mainGroup.removeWidget(this);
                }
            }
        };

        contextMenu.setDynamicSized(true);
        contextMenu.setLayout(Layout.VERTICAL_LEFT);
        contextMenu.setBackground(new ColorRectAndBorderTexture(0xFF1E1E1E, 1, 0xFF555555));

        final var button = new ButtonWidget(0, 0, 120, 20, new TextTexture(() -> I18n.get("client.shop.component.editor.component_selector.widget.tile.copy_id")), (s) -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(type.getId().toString());
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("Copied!"));
            mainGroup.removeWidget(contextMenu);
        });
        button.initTemplate();
        contextMenu.addWidget(button);

        mainGroup.addWidget(contextMenu);
        contextMenu.setFocus(true);
    }
}
