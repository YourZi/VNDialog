package top.yourzi.dialog.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import top.yourzi.dialog.Config;
import top.yourzi.dialog.Dialog;
import top.yourzi.dialog.DialogManager;
import top.yourzi.dialog.model.DialogEntry;
import top.yourzi.dialog.model.DialogOption;
import top.yourzi.dialog.model.DialogSequence;
import top.yourzi.dialog.model.PortraitAnimationType;
import top.yourzi.dialog.model.PortraitPosition;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import java.util.Optional;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import net.minecraft.client.gui.components.Button;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话界面，用于显示对话框和立绘
 */
public class DialogScreen extends Screen {
    //存储每个立绘的显示数据
    private static class PortraitDisplayData {
        ResourceLocation resourceLocation;
        int actualWidth;
        int actualHeight;
        float brightness = 1.0f;
        PortraitPosition position;
        PortraitAnimationType animationType = PortraitAnimationType.NONE;
        long animationStartTime = -1;
        boolean loadedSuccessfully = false;

        PortraitDisplayData(String path, float brightness, PortraitPosition position, PortraitAnimationType animationType) {
            if (path != null && !path.isEmpty()) {
                this.resourceLocation = new ResourceLocation(Dialog.MODID, "textures/portraits/" + path);
                this.brightness = brightness;
                this.position = position != null ? position : PortraitPosition.CENTER; // 默认位置
                this.animationType = animationType != null ? animationType : PortraitAnimationType.NONE; // 存储动画类型
                loadDimensions();
                if (Config.ENABLE_PORTRAIT_ANIMATIONS.get() && loadedSuccessfully && this.animationType != PortraitAnimationType.NONE) {
                    this.animationStartTime = System.currentTimeMillis();
                }
            } else {
                Dialog.LOGGER.warn("Portrait path is null or empty. Cannot load portrait.");
            }
        }

        private void loadDimensions() {
            if (this.resourceLocation == null) return;
            try {
                if (Minecraft.getInstance() != null && Minecraft.getInstance().getResourceManager() != null) {
                    Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(this.resourceLocation);
                    if (resourceOptional.isPresent()) {
                        try (InputStream inputStream = resourceOptional.get().open()) {
                            BufferedImage image = ImageIO.read(inputStream);
                            if (image != null) {
                                this.actualWidth = image.getWidth();
                                this.actualHeight = image.getHeight();
                                this.loadedSuccessfully = true;
                                Dialog.LOGGER.info("Loaded portrait {} with actual dimensions: {}x{}", this.resourceLocation, this.actualWidth, this.actualHeight);
                            } else {
                                Dialog.LOGGER.warn("Could not decode image for portrait: {}.", this.resourceLocation);
                            }
                        }
                    } else {
                        Dialog.LOGGER.warn("Portrait resource not found: {}.", this.resourceLocation);
                    }
                } else {
                    Dialog.LOGGER.warn("Minecraft instance or ResourceManager not available for portrait loading.");
                }
            } catch (IOException e) {
                Dialog.LOGGER.error("Error reading portrait image {}: {}.", this.resourceLocation, e.getMessage());
            } catch (Exception e) {
                Dialog.LOGGER.error("Unexpected error loading portrait image {}: {}.", this.resourceLocation, e.getMessage());
            }
        }
    }

    // 对话序列和当前对话条目
    private final DialogSequence dialogSequence;
    private final DialogEntry dialogEntry;

    // 对话框位置和大小
    private int dialogBoxX;
    private int dialogBoxY;
    private int dialogBoxWidth;
    private int dialogBoxHeight;

    // 选项按钮列表
    private final List<OptionButton> optionButtons = new ArrayList<>();


    //立绘数据列表
    private final List<PortraitDisplayData> portraitDisplayList = new ArrayList<>();

    private static final int ANIMATION_DURATION_MS = 300; // 动画持续时间，单位毫秒

    // 文本动画相关
    private int currentCharIndex = 0;
    private long lastCharTime = 0;
    private boolean textFullyDisplayed = false;

    // 快速跳过相关
    private int fastForwardCooldown = 0;
    private boolean optionButtonsCreated = false; // 标记选项按钮是否已为当前条目创建

    // 对话历史记录界面相关
    private boolean showingHistory = false;
    private int historyScrollOffset = 0;
    private List<DialogEntry> historyEntries = new ArrayList<>();
    private static final int HISTORY_MAX_LINES_DISPLAYED = Config.MAX_HISTORY_LINES.get(); // 历史记录界面一次显示的最大行数
    private Button closeHistoryButton; // 关闭历史记录按钮
    private Button viewHistoryButton; // 查看历史按钮
    private Button autoPlayButton; // 自动播放按钮

    public DialogScreen(DialogSequence dialogSequence, DialogEntry dialogEntry) {
        super(dialogEntry.getSpeaker() != null ? dialogEntry.getSpeaker() : Component.empty());
        this.dialogSequence = dialogSequence;
        this.dialogEntry = dialogEntry;

        // 加载多个立绘资源
        if (dialogEntry.getPortraits() != null && !dialogEntry.getPortraits().isEmpty()) {
            for (top.yourzi.dialog.model.PortraitInfo portraitInfo : dialogEntry.getPortraits()) {
                if (portraitInfo.getPath() != null && !portraitInfo.getPath().isEmpty()) {
                    PortraitDisplayData displayData = new PortraitDisplayData(
                        portraitInfo.getPath(),
                        portraitInfo.getBrightness(),
                        portraitInfo.getPosition(),
                        portraitInfo.getAnimationType() // 传递动画类型
                    );
                    if (displayData.loadedSuccessfully) {
                        this.portraitDisplayList.add(displayData);
                    }
                } else {
                    Dialog.LOGGER.warn("Encountered a portrait info with null or empty path.");
                }
            }
        } else {
            Dialog.LOGGER.info("No portrait configurations found in DialogEntry or the list is empty.");
        }

        // 检查是否由快速跳过触发
        if (DialogManager.isFastForwardingNext()) {
            this.fastForwardCooldown = 5;
            DialogManager.setFastForwardingNext(false); // 重置标记
        }
    }

    @Override
    protected void init() {
        super.init();
        
        // 设置对话框位置和大小
        dialogBoxWidth = Config.DIALOG_BOX_WIDTH.get();
        dialogBoxHeight = Config.DIALOG_BOX_HEIGHT.get();
        dialogBoxX = (width - dialogBoxWidth) / 2;
        dialogBoxY = height - dialogBoxHeight - 20;

        // 初始化查看历史按钮 (位于对话框右下角)
        int historyButtonWidth = 20;
        int historyButtonHeight = 20;
        int historyButtonPadding = 5;
        this.viewHistoryButton = Button.builder(Component.literal("▲"), (button) -> {
            toggleHistoryScreen();
        }).bounds(dialogBoxX + dialogBoxWidth - historyButtonWidth - historyButtonPadding, 
                  dialogBoxY + dialogBoxHeight - historyButtonHeight - historyButtonPadding, 
                  historyButtonWidth, historyButtonHeight).build();
        this.addRenderableWidget(this.viewHistoryButton);

        // 初始化自动播放按钮 (位于历史记录按钮左侧)
        int autoPlayButtonWidth = 20;
        int autoPlayButtonHeight = 20;
        this.autoPlayButton = Button.builder(Component.literal("▶"), (button) -> {
            toggleAutoPlay();
        }).bounds(dialogBoxX + dialogBoxWidth - historyButtonWidth - historyButtonPadding - autoPlayButtonWidth - historyButtonPadding, 
                  dialogBoxY + dialogBoxHeight - autoPlayButtonHeight - historyButtonPadding, 
                  autoPlayButtonWidth, autoPlayButtonHeight).build();
        this.addRenderableWidget(this.autoPlayButton);
        updateAutoPlayButtonText(); // 初始化按钮文本
        
        // 如果此对话条目有选项，预先停止自动播放
        if (dialogEntry.hasOptions()) {
            if (DialogManager.isAutoPlaying()) {
                DialogManager.stopAutoPlay();
                updateAutoPlayButtonText(); // 更新按钮文本以反映自动播放已停止
            }
        }
        this.optionButtonsCreated = false; // 初始化选项按钮创建标记

        // 初始化关闭历史记录按钮 (用于关闭历史查看界面)
        this.closeHistoryButton = Button.builder(Component.literal("▼"), (button) -> {
            toggleHistoryScreen();
        }).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build();

    }
    
    /**
     * 创建对话选项按钮
     */
    private void createOptionButtons() {
        optionButtons.clear();
        
        DialogOption[] options = dialogEntry.getOptions();
        if (options == null || options.length == 0) {
            return;
        }
        
        int buttonWidth = 200;
        int buttonHeight = 20;
        int buttonSpacing = 5;
        int totalHeight = options.length * (buttonHeight + buttonSpacing) - buttonSpacing;
        int startY = dialogBoxY - totalHeight - 10;
        
        for (int i = 0; i < options.length; i++) {
            DialogOption option = options[i];
            int buttonY = startY + i * (buttonHeight + buttonSpacing);
            
            OptionButton button = new OptionButton(
                    (width - buttonWidth) / 2,
                    buttonY,
                    buttonWidth, buttonHeight,
                    // 使用原始文本，因为选项文本是从对话数据中动态加载的
                    option.getText(),
                    b -> {
                        // 执行选项指令（如果存在）
                        if (option.getCommand() != null && !option.getCommand().isEmpty()) {
                            DialogManager.getInstance().executeCommand(option.getCommand());
                        }
                        DialogManager.getInstance().recordChoiceForCurrentDialog(option.getText().getString());
                        DialogManager.getInstance().jumpToDialog(option.getTargetId());
                    }
            );
            
            optionButtons.add(button);
            addRenderableWidget(button);
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {

        // 如果正在显示历史记录，则渲染历史记录界面
        if (showingHistory) {
            renderHistoryScreen(guiGraphics, mouseX, mouseY, partialTicks);
            // 渲染历史记录界面的关闭按钮
            this.closeHistoryButton.render(guiGraphics, mouseX, mouseY, partialTicks);
            return; // 不渲染对话框和立绘
        }

        // 渲染立绘
        if (!portraitDisplayList.isEmpty()) {
            for (PortraitDisplayData displayData : portraitDisplayList) {
                if (displayData.loadedSuccessfully && displayData.resourceLocation != null && displayData.actualWidth > 0 && displayData.actualHeight > 0) {
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);

                    RenderSystem.setShaderTexture(0, displayData.resourceLocation);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();

                    int portraitRenderHeight = (int) (this.height * 0.7); // 固定高度
                    float aspectRatio = (float) displayData.actualWidth / displayData.actualHeight;
                    int portraitRenderWidth = (int) (portraitRenderHeight * aspectRatio); // 等比例计算宽度

                    int baseX = 0, baseY = 0;
                    float currentScale = 1.0f;
                    float currentAlpha = 1.0f;
                    float yOffset = 0;
                    float xOffset = 0;

                    long currentTime = System.currentTimeMillis();
                    float progress = 1.0f;

                    if (Config.ENABLE_PORTRAIT_ANIMATIONS.get() && displayData.animationType != PortraitAnimationType.NONE && displayData.animationStartTime != -1) {
                        long elapsedTime = currentTime - displayData.animationStartTime;
                        if (elapsedTime < ANIMATION_DURATION_MS) {
                            progress = (float) elapsedTime / ANIMATION_DURATION_MS;
                        } else {
                            displayData.animationStartTime = -1;
                        }

                        switch (displayData.animationType) {
                            case FADE_IN:
                                currentAlpha = Mth.lerp(progress, 0f, 1f);
                                break;
                            case SLIDE_IN_FROM_BOTTOM:
                                yOffset = Mth.lerp(progress, 50f, 0f);
                                break;
                            case BOUNCE:
                                if (progress < 0.5f) {
                                    yOffset = Mth.lerp(progress * 2, 0f, -20f);
                                } else {
                                    yOffset = Mth.lerp((progress - 0.5f) * 2, -20f, 0f);
                                }
                                break;
                            case NONE:
                            default:
                                break;
                        }
                    }
                    if (displayData.brightness == 0.0f) {
                        RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 1.0f); // 纯黑剪影，完全不透明
                    } else {
                        //使用brightness调整RGB，currentAlpha处理透明度
                        RenderSystem.setShaderColor(displayData.brightness, displayData.brightness, displayData.brightness, currentAlpha);
                    }

                    int scaledWidth = (int) (portraitRenderWidth * currentScale);
                    int scaledHeight = (int) (portraitRenderHeight * currentScale);

                    switch (displayData.position) {
                        case LEFT:
                            baseX = 20;
                            baseY = this.height - scaledHeight;
                            break;
                        case RIGHT:
                            baseX = this.width - scaledWidth - 20;
                            baseY = this.height - scaledHeight;
                            break;
                        case CENTER:
                        default:
                            baseX = (this.width - scaledWidth) / 2;
                            baseY = this.height - scaledHeight;
                            break;
                    }

                    int finalX = baseX + (int)xOffset;
                    int finalY = baseY + (int)yOffset;

                    guiGraphics.blit(displayData.resourceLocation, finalX, finalY, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
                    RenderSystem.disableBlend();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // 重置颜色
                }
            }
        }

        // 渲染对话框背景
        String backgroundImagePath = "textures/dialog_background/background.png";
        if (backgroundImagePath != null && !backgroundImagePath.isEmpty()) {
            try {
                ResourceLocation dialogBgRl = new ResourceLocation(top.yourzi.dialog.Dialog.MODID, backgroundImagePath);
                
                RenderSystem.setShader(GameRenderer::getPositionTexShader); // 确保使用正确的着色器
                RenderSystem.setShaderTexture(0, dialogBgRl); // 绑定纹理
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // 重置颜色，确保图片不受先前渲染影响
                RenderSystem.enableBlend(); // 为透明图片启用混合
                RenderSystem.defaultBlendFunc(); // 使用默认混合函数

                // 将图片拉伸至对话框大小进行渲染
                // blit(texture, x, y, zLevel, uOffset, vOffset, widthOnScreen, heightOnScreen, textureRegionWidth, textureRegionHeight)
                guiGraphics.blit(dialogBgRl, dialogBoxX, dialogBoxY, 0, 0.0F, 0.0F, dialogBoxWidth, dialogBoxHeight, dialogBoxWidth, dialogBoxHeight);
                
                RenderSystem.disableBlend(); // 绘制完毕后禁用混合
            } catch (Exception e) {
                top.yourzi.dialog.Dialog.LOGGER.error("Failed to render dialog background image: " + backgroundImagePath + ". Falling back to solid color.", e);
                // 回退到纯色背景
                int backgroundColor = Config.DIALOG_BACKGROUND_COLOR.get();
                int opacity = Config.DIALOG_BACKGROUND_OPACITY.get();
                int color = (opacity << 24) | (backgroundColor & 0xFFFFFF);
                guiGraphics.fill(dialogBoxX, dialogBoxY, dialogBoxX + dialogBoxWidth, dialogBoxY + dialogBoxHeight, color);
            }
        } else {

            int backgroundColor = Config.DIALOG_BACKGROUND_COLOR.get();
            int opacity = Config.DIALOG_BACKGROUND_OPACITY.get();
            int color = (opacity << 24) | (backgroundColor & 0xFFFFFF);
            guiGraphics.fill(dialogBoxX, dialogBoxY, dialogBoxX + dialogBoxWidth, dialogBoxY + dialogBoxHeight, color);
        }

        // 如果自动播放开启且无选项，显示提示
        if (DialogManager.isAutoPlaying() && !dialogEntry.hasOptions()) {
            Component autoPlayText = Component.literal("[AUTO]");
            int autoPlayTextWidth = this.font.width(autoPlayText);
            // 将提示显示在对话框的右上角外部一点或者左上角，避免遮挡按钮
            guiGraphics.drawString(this.font, autoPlayText, dialogBoxX + dialogBoxWidth - autoPlayTextWidth - 5, dialogBoxY - 15, 0xFFFFFF);
        }
        
        // 渲染对话文本
        int padding = Config.DIALOG_BOX_PADDING.get();
        int textX = dialogBoxX + padding;
        int textY = dialogBoxY + padding;
        
        // 如果显示说话者名称且有说话者
        Component speakerComponent = dialogEntry.getSpeaker();
        if (Config.SHOW_SPEAKER_NAME.get() && speakerComponent != null && !speakerComponent.getString().isEmpty()) {
            guiGraphics.drawString(font, Component.literal("[").append(speakerComponent).append("]"), textX, textY, 0xFFFFFF);
            textY += font.lineHeight + 5;
        }
        
        // 渲染对话文本
        String rawText = dialogEntry.getText().getString();
        if (rawText != null && !rawText.isEmpty()) {
            int maxWidth = dialogBoxWidth - (padding * 2);
            int textAnimationSpeed = Config.TEXT_ANIMATION_SPEED.get(); // 每秒字符数

            if (textAnimationSpeed <= 0) { // 立即显示
                textFullyDisplayed = true;
                currentCharIndex = rawText.length();
            }

            if (!textFullyDisplayed) {
                long currentTime = System.currentTimeMillis();
                if (lastCharTime == 0) { // 首次渲染或重置
                    lastCharTime = currentTime;
                }
                // 计算每字符间隔时间 (毫秒)
                long charInterval = (textAnimationSpeed > 0) ? (1000 / textAnimationSpeed) : 0;
                
                if (currentTime - lastCharTime >= charInterval) {
                    currentCharIndex++;
                    lastCharTime = currentTime;
                    if (currentCharIndex >= rawText.length()) {
                        textFullyDisplayed = true;
                        currentCharIndex = rawText.length(); // 确保索引不超过长度
                        lastCharTime = System.currentTimeMillis(); // 记录文本完全显示的时间点，用于自动播放计时
                    }
                }
            }

            // 如果自动播放开启，且文本完全显示，且没有选项，则延迟后自动前进
            if (DialogManager.isAutoPlaying() && textFullyDisplayed && !dialogEntry.hasOptions()) {
                if (System.currentTimeMillis() - lastCharTime > Config.AUTO_ADVANCE_DELAY.get()) { // lastCharTime 在文本完全显示后更新
                    // 执行当前对话条目的指令（如果存在）
                    if (dialogEntry.getCommand() != null && !dialogEntry.getCommand().isEmpty()) {
                        DialogManager.getInstance().executeCommand(dialogEntry.getCommand());
                    }
                    DialogManager.getInstance().showNextDialog();
                    return;
                }
            }
            
            List<net.minecraft.util.FormattedCharSequence> lines;
            if (textFullyDisplayed) {
                lines = font.split(dialogEntry.getText(), maxWidth);
            } else {
                String animatedString = rawText.substring(0, Math.min(currentCharIndex, rawText.length()));
                if (animatedString.isEmpty()) {
                    lines = java.util.Collections.emptyList();
                } else {
                    Component animatedTextComponent = Component.literal(animatedString);
                    lines = font.split(animatedTextComponent, maxWidth);
                }
            }
            
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                guiGraphics.drawString(font, line, textX, textY, Config.DIALOG_TEXT_COLOR.get());
                textY += font.lineHeight;
            }
        }

        // 在文本完全显示后，并且有选项时，才创建和显示选项按钮
        if (textFullyDisplayed && dialogEntry.hasOptions()) {
            if (!this.optionButtonsCreated) {
                createOptionButtons();
                this.optionButtonsCreated = true;
            }
        }
        // 渲染按钮和其他UI元素
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // 处理快速跳过
        boolean isCtrlPressed = Minecraft.getInstance().getWindow() != null &&
                                (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                                 GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS);

        // 如果按下Ctrl键快速跳过，则关闭自动播放
        if (isCtrlPressed && DialogManager.isAutoPlaying()) {
            DialogManager.stopAutoPlay();
            updateAutoPlayButtonText();
        }

        if (isCtrlPressed && !dialogEntry.hasOptions()) {
            if (fastForwardCooldown > 0) {
                fastForwardCooldown--;
            } else {
                DialogManager.setFastForwardingNext(true);
                DialogManager.getInstance().showNextDialog();
                return; // 立即跳到下一条，避免渲染当前帧的剩余部分
            }
        } else {
            // 如果Ctrl未按下或有选项，则清除快速跳过标记，确保正常流程
            DialogManager.setFastForwardingNext(false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return Config.IS_PAUSE_SCREEN.get();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        // 如果正在显示历史记录，则按键事件由历史记录界面处理
        if (showingHistory) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_H) {
                toggleHistoryScreen();
                return true;
            }
            return false; // 其他按键在历史记录界面不处理
        }

        // 当文本完全显示，且没有选项时，按空格键或回车键可以手动前进
        if (textFullyDisplayed && !dialogEntry.hasOptions() && (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER)) {
             // 如果按键，则关闭自动播放
            if (DialogManager.isAutoPlaying()) {
                DialogManager.stopAutoPlay();
                updateAutoPlayButtonText();
            }
            // 执行当前对话条目的指令（如果存在）
            if (dialogEntry.getCommand() != null && !dialogEntry.getCommand().isEmpty()) {
                DialogManager.getInstance().executeCommand(dialogEntry.getCommand());
            }
            DialogManager.getInstance().showNextDialog();
            return true;
        }

        // 如果文本未完全显示，按空格或回车则立即显示全部文本
        if (!textFullyDisplayed && (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER)) {
            // 如果按键，则关闭自动播放
            if (DialogManager.isAutoPlaying()) {
                DialogManager.stopAutoPlay();
                updateAutoPlayButtonText();
            }
            textFullyDisplayed = true;
            currentCharIndex = dialogEntry.getText().getString().length();
            lastCharTime = System.currentTimeMillis(); // 更新时间，以便自动播放计时器正确工作
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 如果点击，则关闭自动播放
        if (DialogManager.isAutoPlaying()) {
            DialogManager.stopAutoPlay();
            updateAutoPlayButtonText();
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // 如果没有显示历史记录且没有 widget 处理点击事件，
        // 则检查是否点击了对话框区域以推进文本/对话。
        if (!showingHistory) {
            // 检查点击是否在对话框边界内
            boolean clickedInDialogBox = button == 0 &&
                                         dialogBoxX <= mouseX && mouseX <= dialogBoxX + dialogBoxWidth &&
                                         dialogBoxY <= mouseY && mouseY <= dialogBoxY + dialogBoxHeight;

            if (clickedInDialogBox) {
                if (!textFullyDisplayed) {
                    // 如果文本未完全显示，点击使其完全显示
                    textFullyDisplayed = true;
                    currentCharIndex = dialogEntry.getText().getString().length();
                    lastCharTime = 0; // 重置动画或自动播放的时间
                    return true; // 消费点击事件
                } else {
                    // 文本已完全显示
                    if (!dialogEntry.hasOptions()) {
                        // 如果没有选项，则推进对话
                        // 执行当前对话条目的指令（如果存在）
                        if (dialogEntry.getCommand() != null && !dialogEntry.getCommand().isEmpty()) {
                            DialogManager.getInstance().executeCommand(dialogEntry.getCommand());
                        }
                        DialogManager.getInstance().showNextDialog();
                        return true; // 消费点击事件
                    }
                    // 如果有选项，此点击不是用于推进对话。
                    // 选项按钮的点击由 super.mouseClicked 处理。
                    // 如果是对话框内的点击但未在选项按钮上，
                    // 并且文本已完全显示且存在选项，则此处不执行任何操作。
                }
            }
        }
        
        return false; // 除了 widgets 或对话推进之外没有自定义处理
    }

    /**
     * 切换对话历史记录界面的显示状态
     */
    public void toggleHistoryScreen() {
        this.showingHistory = !this.showingHistory;
    }

    private void toggleAutoPlay() {
        DialogManager.setAutoPlaying(!DialogManager.isAutoPlaying());
        updateAutoPlayButtonText();
    }

    private void updateAutoPlayButtonText() {
        if (this.autoPlayButton != null) {
            this.autoPlayButton.setMessage(Component.literal(DialogManager.isAutoPlaying() ? "⏸" : "▶"));
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateAutoPlayButtonText(); 
    
        if (this.showingHistory) {
            this.historyEntries = DialogManager.getInstance().getDialogHistory();
            // 禁用主对话界面按钮
            this.optionButtons.forEach(b -> b.active = false);
            if (this.viewHistoryButton != null) { // 确保按钮已初始化
                this.viewHistoryButton.active = false;
            }
            
            // 激活并添加关闭历史按钮
            if (!this.children().contains(this.closeHistoryButton)) {
                 this.addRenderableWidget(this.closeHistoryButton);
            }
            this.closeHistoryButton.active = true;

        } else {
            // 恢复主对话界面按钮
            this.optionButtons.forEach(b -> b.active = true);
            if (this.viewHistoryButton != null) { // 确保按钮已初始化
                this.viewHistoryButton.active = true;
            }
            
            // 移除关闭历史按钮
            if (this.children().contains(this.closeHistoryButton)) {
                this.removeWidget(this.closeHistoryButton);
            }
            this.closeHistoryButton.active = false; 
        }
    }

    /**
     * 渲染对话历史记录界面
     */
    private void renderHistoryScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        guiGraphics.fill(0, 0, this.width, this.height, 0xCC000000); // 半透明黑色背景

        int currentY = 35;
        final int textPaddingLeft = 50;
        final int optionPaddingLeft = textPaddingLeft + 10;
        final int baseLineSpacing = font.lineHeight + 7;
        final int extraEmptyLineHeight = font.lineHeight;

        int entriesToShow = Math.min(HISTORY_MAX_LINES_DISPLAYED, historyEntries.size());

        for (int i = 0; i < entriesToShow; i++) {
            int historyIndex = historyScrollOffset + i;
            
            if (historyIndex >= historyEntries.size()) break; 

            DialogEntry entry = historyEntries.get(historyIndex);
            Component currentEntrySpeaker = entry.getSpeaker();
            Component dialogText = entry.getText();
            Component lineToRender;

            // 确保 dialogText 不为 null，以防止在 append 时出现 NullPointerException
            if (dialogText == null) {
                dialogText = Component.empty();
            }

            if (currentEntrySpeaker != null && !currentEntrySpeaker.getString().isEmpty()) {
                // 使用 Component.literal
                lineToRender = Component.literal("[").append(currentEntrySpeaker).append("] ").append(dialogText);
            } else {
                lineToRender = dialogText;
            }
            
            // 确保 lineToRender 不为 null 才进行绘制
            if (lineToRender != null) {
                guiGraphics.drawString(font, lineToRender, textPaddingLeft, currentY, 0xFFFFFF);
            }
            currentY += baseLineSpacing;

            // 显示选择的选项
            if (entry.getSelectedOptionText() != null && !entry.getSelectedOptionText().isEmpty()) {
                String selectedOptionDisplay = " -> " + entry.getSelectedOptionText();
                guiGraphics.drawString(font, selectedOptionDisplay, optionPaddingLeft, currentY + 5, 0xAAAAAA);
                currentY += baseLineSpacing;
                currentY += extraEmptyLineHeight;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showingHistory) {
            if (delta < 0) { // 向下滚动
                if (historyScrollOffset < historyEntries.size() - HISTORY_MAX_LINES_DISPLAYED) {
                    historyScrollOffset++;
                }
            } else { // 向上滚动
                if (historyScrollOffset > 0) {
                    historyScrollOffset--;
                }
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}