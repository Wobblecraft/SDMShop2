package dev.sixik.sdmshop2.libs.shop.client;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

/**
 * Hook для {@link com.lowdragmc.lowdraglib.gui.widget.WidgetGroup} чтобы возможно было обновлять весь UI
 */
public interface WidgetGroupAccessor {

    static WidgetGroupAccessor get(WidgetGroup group) {
        return (WidgetGroupAccessor)group;
    }

    /**
     * Заставляет {@code LDLib} полностью обновить весь UI
     *
     * @param child Инициатор обновления
     */
    void sdm$onChildSizeUpdate(Widget child);
}
