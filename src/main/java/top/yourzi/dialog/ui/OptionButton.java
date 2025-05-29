package top.yourzi.dialog.ui;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 对话选项按钮
 */
public class OptionButton extends ImageButton {

    public OptionButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffText, 
            ResourceLocation resourceLocation, int textureWidth, int textureHeight, 
            Button.OnPress onPress, Component message) {
        super(x, y, width, height, xTexStart, yTexStart, yDiffText, resourceLocation, textureWidth, textureHeight, 
              onPress, message);
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