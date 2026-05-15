package dev.sixik.sdmshop2.libs.shop.client.config;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.texture.TransformTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.gui.widget.layout.Layout;
import com.lowdragmc.lowdraglib.utils.Size;
import dev.sixik.sdmshop2.libs.shop.client.WidgetGroupAccessor;
import dev.sixik.sdmshop2.libs.shop.client.config.constructors.ComponentConfigAccess;
import dev.sixik.sdmshop2.libs.shop.client.config.constructors.ComponentConfigWidgetConstructor;
import dev.sixik.sdmshop2.libs.shop.client.screens.widgets.ExternTextFieldWidget;
import dev.sixik.sdmshop2.libs.shop.client.screens.widgets.SDMTextLabel;
import dev.sixik.sdmshop2.libs.shop.client.textures.ColorRectAndBorderTexture;
import dev.sixik.sdmshop2.libs.shop.components.api.ShopComponent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class ComponentConfigurationWidget extends WidgetGroup {

    public static final ColorRectAndBorderTexture texture = new ColorRectAndBorderTexture();
    public static final ColorBorderTexture hoverTexture = new ColorBorderTexture(1, -1);

    protected final List<Widget[]> uiPairs = new ArrayList<>();

    @Getter
    @Nullable
    protected ShopComponent component;

    @Setter
    protected BiConsumer<Integer, SwitchWidget> modifySwitchWidgetCallback = (index, widget) -> {
        widget.setTexture(new GuiTextureGroup(getTexture(), new TextTexture("off")), new GuiTextureGroup(getTexture(), new TextTexture("on")));
    };

    @Setter
    protected BiConsumer<Integer, ExternTextFieldWidget> modifyExternTextFieldWidgetCallback = (index, widget) -> {
        widget.setTextFieldHeight(20);
    };

    @Setter
    protected BiConsumer<Integer, SDMTextLabel> modifyTextLabelCreateCallback = (index, widget) -> { };

    @Setter
    protected ModifyElements modifyInitElementsCallback = ((main, label, editor, font, editorWidth, editorX, currentY) -> {
        editor.setSizeWidth(editorWidth);
        editor.setSelfPosition(editorX, currentY);

        int maxLabelWidth = editorX - 10 - 5;
        int originalTextWidth = font.width(label.getText());

        /*
            Считаем Scale
         */
        float scale = 1.0f;
        if (originalTextWidth > maxLabelWidth && originalTextWidth > 0) {
            /*
                Если текст шире, чем доступное место, считаем коэффициент сжатия.
             */
            scale = (float) maxLabelWidth / originalTextWidth;
            scale = Math.max(scale, 0.4f);
        }

        label.setScale(scale);

        int editorHeight = editor.getSizeHeight();
        float visualTextHeight = font.lineHeight * scale;

        int labelY = currentY + (int)((editorHeight - visualTextHeight) / 2f);

        label.setSelfPosition(10, labelY);

        /*
            Возвращаем шаг по Y для следующего элемента (высота виджета + отступ)
         */
        return editorHeight + 5;
    });

    @Getter @Setter
    protected int fixedWidth;

    public ComponentConfigurationWidget(@Nullable ShopComponent component) {
        this(60, component);
    }

    public ComponentConfigurationWidget(int width, @Nullable ShopComponent component) {
        super(0, 0, width, 0);
        this.fixedWidth = width;
        this.component = component;
        this.setDynamicSized(true);
        this.setLayout(Layout.NONE);

        setBackground(getTexture());
    }

    @Override
    public void initWidget() {
        updateConfiguration();
        super.initWidget();
    }

    public void setComponent(ShopComponent component) {
        if(Objects.equals(component, this.component)) return;
        this.component = component;
        updateConfiguration();
    }

    public void updateConfiguration() {
        clearAllWidgets();
        uiPairs.clear();
        if(component == null) return;

        ObjectArrayList<ComponentConfigAccess.CachedField> fieldsList =
                ComponentConfigAccess.getCachedFields(component.getClass());

        for (int i = 0; i < fieldsList.size(); i++) {
            final var datum = fieldsList.get(i);
            Widget editorWidget = ComponentConfigWidgetConstructor.createWidget(component, datum);
            if (editorWidget == null) continue;
            if(editorWidget instanceof SwitchWidget switchWidget) {
                modifySwitchWidgetCallback.accept(i, switchWidget);
            } else if(editorWidget instanceof ExternTextFieldWidget textFieldWidget) {
                modifyExternTextFieldWidgetCallback.accept(i, textFieldWidget);
            }

            editorWidget.setHoverTexture(getHoverTexture());
            editorWidget.setSizeHeight(20);

            if (!(editorWidget instanceof WidgetGroup)) {
                editorWidget.setSizeHeight(20);
            }

            SDMTextLabel textLabel = new SDMTextLabel(Component.translatable(datum.translationKey()));
            modifyTextLabelCreateCallback.accept(i, textLabel);

            @Nullable String tooltip = datum.tooltipTranslationKey();
            if(tooltip != null && I18n.exists(tooltip)) {
                editorWidget.setHoverTooltips(tooltip);
                textLabel.setHoverTooltips(tooltip);
            }

            uiPairs.add(new Widget[]{textLabel, editorWidget});
        }


        /*
            Самый верхний элемент интерфейса (index 0) будет добавлен последним,
            поэтому его DropDown перекроет все нижние виджеты и заберет клики.
         */
        for (int i = uiPairs.size() - 1; i >= 0; i--) {
            Widget[] pair = uiPairs.get(i);
            this.addWidget(pair[0]); // Label
            this.addWidget(pair[1]); // Editor
        }

        super.initWidget();
        repositionWidgets();
    }

    /**
     * Тот самый метод, который двигает всё "в прямом эфире"
     */
    public void repositionWidgets() {
        int currentY = 10;
        int editorWidth = 85;
        int paddingRight = 10;
        int editorX = getSizeWidth() - editorWidth - paddingRight;
        Font font = Minecraft.getInstance().font;

        for (Widget[] pair : uiPairs) {
            SDMTextLabel label = (SDMTextLabel) pair[0];
            Widget editor = pair[1];
            currentY += modifyInitElementsCallback.accept(this, label, editor, font, editorWidth, editorX, currentY);
        }

        /*
            Заставляем саму группу пересчитать свою высоту
         */
        this.recomputeSize();
    }

    /**
     * Твой переопределенный метод из setSize теперь прилетит сюда
     */
    @Override
    public void onChildSizeUpdate(Widget child) {
        /*
            Пересчитываем координаты существующих виджетов
         */
        repositionWidgets();

        /*
             Пробрасываем уведомление дальше вверх к CollapsedGroupWidget
         */
        if (parent != null) {
            ((WidgetGroupAccessor)parent).sdm$onChildSizeUpdate(this);
        }
    }

    public TransformTexture getTexture() {
        return texture;
    }

    public TransformTexture getHoverTexture() {
        return hoverTexture;
    }

    //////////////////////////////////////////////////////
    ///             FIX SIZE WIDTH                     ///
    //////////////////////////////////////////////////////
    @Override
    public void setSize(Size size) {
        super.setSize(size);
        this.fixedWidth = size.width;
    }

    @Override
    public Size getSize() {
        return new Size(fixedWidth, super.getSize().height);
    }

    @Override
    protected Size computeDynamicSize() {
        Size wrappedSize = super.computeDynamicSize();
        return new Size(fixedWidth, wrappedSize.height);
    }

    public interface ModifyElements {

        int accept(ComponentConfigurationWidget main, SDMTextLabel label, Widget editor, Font font, int editorWidth, int editorX, int currentY);
    }
}
