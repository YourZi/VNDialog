package top.yourzi.dialog.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;

/**
 * 对话选项按钮
 */
public class OptionButton extends ImageButton {

    public OptionButton(int x, int y, int width, int height, WidgetSprites sprites, OnPress onPress,
            Component message) {
        super(x, y, width, height, sprites, onPress, message);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        Component message = this.getMessage();
        if (message != Component.EMPTY) {
            int stringWidth = font.width(message);
            int textColor = this.active ? 0xFFFFFF : 0xA0A0A0;
            int textX = this.getX() + (this.width - stringWidth) / 2;
            int textY = this.getY() + (this.height - font.lineHeight) / 2;
            guiGraphics.drawString(font, message, textX, textY, textColor);
        }
    }
}