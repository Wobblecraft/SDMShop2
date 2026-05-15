package dev.sixik.sdmshop2.libs.shop.client.screens.widgets;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class SDMWrappedTextLabel extends Widget {

    @Getter
    protected Component text;

    @Getter
    protected float scale = 1.0f;

    @Getter
    protected int maxWidth;

    public int color = 0xFFFFFFFF;

    protected ObjectArrayList<FormattedCharSequence> cachedLines = new ObjectArrayList<>();

    public SDMWrappedTextLabel(Component text, int maxWidth) {
        this(0, 0, maxWidth, text);
    }

    /**
     * @param maxWidth Максимальная ширина виджета на экране, после которой текст будет переноситься
     */
    public SDMWrappedTextLabel(int x, int y, int maxWidth, Component text) {
        super(x, y, maxWidth, 0); // Высоту мы вычислим динамически
        this.maxWidth = maxWidth;
        this.text = text;
        updateLinesAndSize();
    }

    public void setText(Component text) {
        this.text = text;
        updateLinesAndSize();
    }

    public void setScale(float scale) {
        this.scale = scale;
        updateLinesAndSize();
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        updateLinesAndSize();
    }

    /**
     * Разбивает текст на строки и пересчитывает высоту виджета.
     * Благодаря твоему переопределенному setSize, это автоматически обновит родительские Layout-ы.
     */
    private void updateLinesAndSize() {
        if (this.text == null) return;

        Font font = Minecraft.getInstance().font;

        /*
             Если масштаб меньше 1, реального места для шрифта больше.
             Например, при ширине 100 и scale 0.5, мы можем впихнуть 200 "пикселей" текста в одну строку.
         */
        int unscaledMaxWidth = (int) (this.maxWidth / this.scale);
        this.cachedLines = new ObjectArrayList<>(font.split(this.text, Math.max(1, unscaledMaxWidth)));

        /*
            Вычисляем финальную высоту: количество строк * высоту строки * масштаб
         */
        int calculatedHeight = (int) (this.cachedLines.size() * font.lineHeight * this.scale);
        this.setSize(new Size(this.maxWidth, calculatedHeight));
    }

    @Override
    public void drawInBackground(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);

        if (this.cachedLines.isEmpty()) return;

        var position = getPosition();
        Font font = Minecraft.getInstance().font;
        PoseStack poseStack = graphics.pose();

        poseStack.pushPose();

        poseStack.translate(position.getX(), position.getY(), 0);

        if (this.scale != 1.0f) {
            poseStack.scale(scale, scale, 1.0f);
        }

        int currentY = 0;

        final Object[] simpleArray = cachedLines.elements();
        for (int i = 0; i < cachedLines.size(); i++) {
            graphics.drawString(font, (FormattedCharSequence) simpleArray[i], 0, currentY, color, false);
            currentY += font.lineHeight;
        }

        poseStack.popPose();
    }
}
