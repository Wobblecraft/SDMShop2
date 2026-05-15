package dev.sixik.sdmshop2.libs.shop.client.config;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.layout.Layout;
import dev.sixik.sdmshop2.SDMShop2;
import dev.sixik.sdmshop2.libs.shop.base.ShopEntity;
import dev.sixik.sdmshop2.libs.shop.client.WidgetGroupAccessor;
import dev.sixik.sdmshop2.libs.shop.client.config.component_selector.ComponentSelectionMenu;
import dev.sixik.sdmshop2.libs.shop.client.screens.widgets.CollapsedGroupWidget;
import dev.sixik.sdmshop2.libs.shop.client.textures.ColorRectAndBorderTexture;
import dev.sixik.sdmshop2.libs.shop.components.api.ShopComponent;
import dev.sixik.sdmshop2.libs.shop.components.api.ShopComponentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ComponentCollapsedGroupWidget extends CollapsedGroupWidget {

    protected ShopComponent component;
    protected ShopEntity root;

    public ComponentCollapsedGroupWidget(ShopComponent component, ShopEntity root, int width) {
        super(Component.literal(component.getType().getId().toString()), width);
        this.component = component;
        this.root = root;
    }

    /**
     * Обработка клика по шапке
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = getPositionX();
        int y = getPositionY();


        if (isMouseOver(x, y, getSizeWidth(), headerHeight, mouseX, mouseY)) {
            if (canCollapse && button == 0) {
                setCollapsed(!isCollapsed);
                Widget.playButtonClickSound();
                return true;
            }

            if (button == 1) {
                openContextMenu((int) mouseX, (int) mouseY);
                Widget.playButtonClickSound();
                return true;
            }
        }

        if (isCollapsed) return false;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void drawInForeground(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);

        int x = getPositionX();
        int y = getPositionY();

        if (isMouseOver(x, y, getSizeWidth(), headerHeight, mouseX, mouseY) && gui != null && gui.getModularUIGui() != null) {

            for (Widget widget : widgets) {
                if (widget instanceof ComponentConfigurationWidget configWidget) {
                    ShopComponent component = configWidget.getComponent();

                    if (component != null) {
                        List<Component> tooltip = new ArrayList<>();
                        tooltip.add(Component.translatable("client.shop.component.editor.json.preivew")); // §e - желтый цвет

                        try {
                            JsonObject json = ShopComponentRegistry.toJson(component);
                            String formattedJson = SDMShop2.GSON.toJson(json);
                            for (String line : formattedJson.split("\n")) {
                                tooltip.add(Component.literal("§7" + line.replace("  ", " ")));
                            }
                        } catch (Exception e) {
                            tooltip.add(Component.translatable("client.shop.component.editor.json.generation_error"));
                        }

                        gui.getModularUIGui().setHoverTooltip(tooltip, ItemStack.EMPTY, null, null);
                    }
                    break;
                }
            }
        }
    }

    protected void openContextMenu(int mouseX, int mouseY) {
        if (this.gui == null) return;

        Widget root = this;
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



        final var button = new ButtonWidget(0, 0, 120, 20, new TextTexture(() -> I18n.get("client.shop.component.editor.json.copy")), (s) -> {
            JsonObject json = ShopComponentRegistry.toJson(component);
            String formattedJson = SDMShop2.GSON.toJson(json);
            Minecraft.getInstance().keyboardHandler.setClipboard(formattedJson);
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("Copied!"));
            mainGroup.removeWidget(contextMenu);
        });
        button.initTemplate();
        contextMenu.addWidget(button);

        final var deleteButton = new ButtonWidget(0, 0, 120, 20, new TextTexture(() -> I18n.get("client.shop.component.editor.components.delete")), s -> {
            if (component.getRoot() != null) {
                component.getRoot().removeComponent(component);
            }

            mainGroup.removeWidget(contextMenu);

            WidgetGroup parentGroup = this.getParent();
            if (parentGroup != null) {
                parentGroup.removeWidget(this);
                ((WidgetGroupAccessor)parentGroup).sdm$onChildSizeUpdate(this);
            }
        });
        deleteButton.initTemplate();
        contextMenu.addWidget(deleteButton);

       /*
       final var testButton = new ButtonWidget(0, 0, 120, 20, new TextTexture("TestButton"), s -> {
            ComponentSelectionMenu.showComponentSelector(getGui().mainGroup, newComponent -> {
                if (newComponent == null) return;
                this.root.addComponent(newComponent);

                WidgetGroup parentGroup = this.getParent();
                if (parentGroup != null) {

                    ComponentCollapsedGroupWidget newWidget = new ComponentCollapsedGroupWidget(
                            newComponent,
                            this.root,
                            this.getSizeWidth()
                    );
                    newWidget.useTabulation();

                    newWidget.addWidget(new ComponentConfigurationWidget(this.getSizeWidth(), newComponent));
                    parentGroup.addWidget(newWidget);

                    *//*
                        Обновление UI
                     *//*
                    WidgetGroupAccessor.get(parentGroup).sdm$onChildSizeUpdate(newWidget);
                }
            });

            *//*
                Закрываем UI
             *//*
            mainGroup.removeWidget(contextMenu);
        });
        testButton.initTemplate();
        contextMenu.addWidget(testButton);
        */

        mainGroup.addWidget(contextMenu);
        contextMenu.setFocus(true);
    }
}
