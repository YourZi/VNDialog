package top.yourzi.dialog.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import top.yourzi.dialog.Dialog;
import top.yourzi.dialog.DialogManager;
import top.yourzi.dialog.model.*;
import top.yourzi.dialog.util.STBBackendImage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * 对话界面，用于显示对话框和立绘
 */
@SuppressWarnings("removal")
public class DialogScreen extends Screen {
    private static final int ANIMATION_DURATION_MS = 300; // 动画持续时间，单位毫秒
    private static final int BACKGROUND_FADE_DURATION_MS = 500; // 背景图片淡入淡出持续时间，单位毫秒
    // 对话序列和当前对话条目
    private final DialogSequence dialogSequence;
    private final DialogEntry dialogEntry;
    // 选项按钮列表
    private final List<OptionButton> optionButtons = new ArrayList<>();
    // 玩家名称
    private final String playerName;
    //立绘数据列表
    private final List<PortraitDisplayData> portraitDisplayList = new ArrayList<>();
    // 需要在对话中显示的物品列表
    private final List<ItemStack> displayItemStacks = new ArrayList<>();
    // 背景图片相关
    private BackgroundImageDisplayData backgroundImageDisplayData;
    private long backgroundFadeStartTime = 0; // 背景图片淡入开始时间
    private long backgroundFadeOutStartTime = 0; // 背景图片淡出开始时间
    private boolean isClosing = false; // 是否正在关闭
    // 对话框位置和大小
    private int dialogBoxX;
    private int dialogBoxY;
    private int dialogBoxWidth;
    private int dialogBoxHeight;
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
    private ImageButton closeHistoryButton; // 关闭历史记录按钮
    private ImageButton viewHistoryButton; // 查看历史按钮
    private ImageButton autoPlayButton; // 自动播放按钮
    // 滚动条相关
    private int totalHistoryContentHeight = 0;
    private boolean canScrollHistoryDown = false;
    private boolean canScrollHistoryUp = false;
    public DialogScreen(DialogSequence dialogSequence, DialogEntry dialogEntry, String playerName) {
        super(dialogEntry.getSpeaker(playerName) != null ? dialogEntry.getSpeaker(playerName) : Component.empty());
        this.dialogSequence = dialogSequence;
        this.dialogEntry = dialogEntry;
        this.playerName = playerName;
        this.font = Minecraft.getInstance().font;

        // 加载背景图片资源
        if (dialogEntry.getBackgroundImage() != null && dialogEntry.getBackgroundImage().getPath() != null && !dialogEntry.getBackgroundImage().getPath().isEmpty()) {
            this.backgroundImageDisplayData = new BackgroundImageDisplayData(dialogEntry.getBackgroundImage());
            // 根据动画类型设置动画开始时间
            if (this.backgroundImageDisplayData.animationType == BackgroundAnimationType.FADE_IN) {
                this.backgroundImageDisplayData.animationStartTime = System.currentTimeMillis();
            }
            this.backgroundFadeStartTime = System.currentTimeMillis(); // 初始化背景淡入开始时间
        }

        // 加载多个立绘资源
        if (dialogEntry.getPortraits() != null && !dialogEntry.getPortraits().isEmpty()) {
            for (top.yourzi.dialog.model.PortraitInfo portraitInfo : dialogEntry.getPortraits()) {
                if (portraitInfo.getPath() != null && !portraitInfo.getPath().isEmpty()) {
                    PortraitDisplayData displayData = new PortraitDisplayData(
                            portraitInfo.getPath(),
                            portraitInfo.getBrightness(),
                            portraitInfo.getPosition(),
                            portraitInfo.getAnimationType()
                    );
                    if (displayData.loadedSuccessfully) {
                        this.portraitDisplayList.add(displayData);
                    }
                } else {
                    Dialog.LOGGER.warn("Encountered a portrait info with null or empty path.");
                }
            }
        } else {
            Dialog.LOGGER.warn("No portrait configurations found in DialogEntry or the list is empty.");
        }

        // 加载需要在对话中显示的物品
        if (dialogEntry.getDisplayItems() != null && !dialogEntry.getDisplayItems().isEmpty()) {
            for (top.yourzi.dialog.model.DisplayItemInfo itemInfo : dialogEntry.getDisplayItems()) {
                if (itemInfo.getItemId() != null && !itemInfo.getItemId().isEmpty()) {
                    try {
                        ResourceLocation itemRl = new ResourceLocation(itemInfo.getItemId());
                        Item item = ForgeRegistries.ITEMS.getValue(itemRl);
                        if (item != null && item != Items.AIR) {
                            ItemStack itemStack = new ItemStack(item, itemInfo.getCount() > 0 ? itemInfo.getCount() : 1);
                            if (itemInfo.getNbt() != null && !itemInfo.getNbt().isEmpty()) {
                                try {
                                    CompoundTag nbtTag = TagParser.parseTag(itemInfo.getNbt());
                                    itemStack.setTag(nbtTag);
                                } catch (Exception e) {
                                    Dialog.LOGGER.error("Error parsing NBT for display item {}: {}. NBT: '{}'", itemInfo.getItemId(), e.getMessage(), itemInfo.getNbt());
                                }
                            }
                            this.displayItemStacks.add(itemStack);
                        } else {
                            Dialog.LOGGER.warn("Item not found or is AIR: {}. Skipping display item.", itemInfo.getItemId());
                        }
                    } catch (Exception e) {
                        Dialog.LOGGER.error("Error creating ItemStack for display item {}: {}", itemInfo.getItemId(), e.getMessage());
                    }
                } else {
                    Dialog.LOGGER.warn("Encountered a display item with null or empty itemId.");
                }
            }
        }

        // 检查是否由快速跳过触发
        if (DialogManager.isFastForwardingNext()) {
            this.fastForwardCooldown = 5;
            DialogManager.setFastForwardingNext(false); // 重置标记
        }
    }

    // 管理背景图片显示数据
    private static class BackgroundImageDisplayData {
        private final ResourceLocation imageLocation;
        private final BackgroundRenderOption renderOption;
        private final BackgroundAnimationType animationType;
        private STBBackendImage image;
        private boolean loadedSuccessfully = false;
        private int imageWidth;
        private int imageHeight;
        private long animationStartTime = -1;

        public BackgroundImageDisplayData(BackgroundImageInfo backgroundImageInfo) {
            this.imageLocation = new ResourceLocation(Dialog.MODID, "textures/backgrounds/" + backgroundImageInfo.getPath());
            this.renderOption = backgroundImageInfo.getRenderOption();
            this.animationType = backgroundImageInfo.getAnimationType() != null ? backgroundImageInfo.getAnimationType() : BackgroundAnimationType.NONE;
            loadResource();
        }

        private void loadResource() {
            try {
                Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(imageLocation);
                if (resourceOptional.isPresent()) {
                    try (InputStream inputStream = resourceOptional.get().open()) {
                        this.image = STBBackendImage.read(inputStream);
                        this.imageWidth = image.getWidth();
                        this.imageHeight = image.getHeight();
                        this.loadedSuccessfully = true;
                    } catch (IOException e) {
                        Dialog.LOGGER.error("Failed to load background image: {}", imageLocation, e);
                    }
                } else {
                    Dialog.LOGGER.warn("Background image resource not found: {}", imageLocation);
                }
            } catch (Exception e) {
                Dialog.LOGGER.error("Error accessing background image resource: {}", imageLocation, e);
            }
        }

        public void close() {
            if (image != null) {
                image.close();
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        // 设置对话框位置和大小
        dialogBoxWidth = top.yourzi.dialog.config.ClientConfig.DIALOG_BOX_WIDTH.get();
        dialogBoxHeight = top.yourzi.dialog.config.ClientConfig.DIALOG_BOX_HEIGHT.get();
        dialogBoxX = (width - dialogBoxWidth) / 2;
        dialogBoxY = height - dialogBoxHeight - 20;

        // 初始化查看历史按钮 (位于对话框右下角)
        int historyButtonWidth = 20;
        int historyButtonHeight = 20;
        int historyButtonPadding = 5;
        
        // 获取按钮纹理信息
        ResourceLocation historyButtonTexture;
        int historyXTexStart = 0;
        int historyYTexStart = 0;
        int historyYDiffText = historyButtonHeight;
        int historyTextureWidth = 256;
        int historyTextureHeight = 128;
        
        // 优先使用本地自定义按钮图集，如果没有则使用原版纹理
        ResourceLocation customButtonAtlas = new ResourceLocation(Dialog.MODID, "textures/buttons/button_atlas.png");
        boolean useCustomTexture = false;
        
        try {
            Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(customButtonAtlas);
            if (resourceOptional.isPresent()) {
                historyButtonTexture = customButtonAtlas;
                useCustomTexture = true;
            } else {
                historyButtonTexture = new ResourceLocation("minecraft", "textures/gui/widgets.png");
            }
        } catch (Exception e) {
            historyButtonTexture = new ResourceLocation("minecraft", "textures/gui/widgets.png");
        }
        
        if (!useCustomTexture) {
            // 原版按钮纹理的起始位置和差值
            historyXTexStart = 0;
            historyYTexStart = 66;
            historyYDiffText = 20;
            historyTextureWidth = 256;
            historyTextureHeight = 256;
        } else {
            // 自定义图集中历史记录按钮的位置 (小按钮区域中间)
            historyXTexStart = 220;
            historyYTexStart = 0;
            historyYDiffText = 20;
            historyTextureWidth = 256;
            historyTextureHeight = 128;
        }
        
        this.viewHistoryButton = new ImageButton(
                dialogBoxX + dialogBoxWidth - historyButtonWidth - historyButtonPadding,
                dialogBoxY + dialogBoxHeight - historyButtonHeight - historyButtonPadding,
                historyButtonWidth, historyButtonHeight,
                historyXTexStart, historyYTexStart, historyYDiffText,
                historyButtonTexture, historyTextureWidth, historyTextureHeight,
                (button) -> toggleHistoryScreen(),
                Component.literal("▲")
        );
        this.addRenderableWidget(this.viewHistoryButton);

        // 初始化自动播放按钮 (位于历史记录按钮左侧)
        int autoPlayButtonWidth = 20;
        int autoPlayButtonHeight = 20;
        
        // 获取自动播放按钮纹理信息
        ResourceLocation autoPlayButtonTexture;
        int autoPlayXTexStart = 0;
        int autoPlayYTexStart = 0;
        int autoPlayYDiffText = autoPlayButtonHeight;
        int autoPlayTextureWidth = 256;
        int autoPlayTextureHeight = 128;
        
        boolean useCustomAutoPlayTexture = false;
        
        try {
            Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(customButtonAtlas);
            if (resourceOptional.isPresent()) {
                autoPlayButtonTexture = customButtonAtlas;
                useCustomAutoPlayTexture = true;
            } else {
                autoPlayButtonTexture = new ResourceLocation("minecraft", "textures/gui/widgets.png");
            }
        } catch (Exception e) {
            autoPlayButtonTexture = new ResourceLocation("minecraft", "textures/gui/widgets.png");
        }
        
        if (!useCustomAutoPlayTexture) {
            // 原版按钮纹理的起始位置和差值
            autoPlayXTexStart = 0;
            autoPlayYTexStart = 66;
            autoPlayYDiffText = 20;
            autoPlayTextureWidth = 256;
            autoPlayTextureHeight = 256;
        } else {
            // 自定义图集中自动播放按钮的位置 (小按钮区域左侧)
            autoPlayXTexStart = 200;
            autoPlayYTexStart = 0;
            autoPlayYDiffText = 20;
            autoPlayTextureWidth = 256;
            autoPlayTextureHeight = 128;
        }
        
        this.autoPlayButton = new ImageButton(
                dialogBoxX + dialogBoxWidth - historyButtonWidth - historyButtonPadding - autoPlayButtonWidth - historyButtonPadding,
                dialogBoxY + dialogBoxHeight - autoPlayButtonHeight - historyButtonPadding,
                autoPlayButtonWidth, autoPlayButtonHeight,
                autoPlayXTexStart, autoPlayYTexStart, autoPlayYDiffText,
                autoPlayButtonTexture, autoPlayTextureWidth, autoPlayTextureHeight,
                (button) -> toggleAutoPlay(),
                Component.literal("▶")
        );
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
        int closeButtonWidth = 60;
        int closeButtonHeight = 20;
        
        // 获取关闭按钮纹理信息
        ResourceLocation closeButtonTexture;
        int closeXTexStart = 0;
        int closeYTexStart = 0;
        int closeYDiffText = closeButtonHeight;
        int closeTextureWidth = 256;
        int closeTextureHeight = 128;
        
        boolean useCustomCloseTexture = false;
        
        try {
            Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(customButtonAtlas);
            if (resourceOptional.isPresent()) {
                closeButtonTexture = customButtonAtlas;
                useCustomCloseTexture = true;
            } else {
                closeButtonTexture = new ResourceLocation("minecraft", "textures/gui/widgets.png");
            }
        } catch (Exception e) {
            closeButtonTexture = new ResourceLocation("minecraft", "textures/gui/widgets.png");
        }
        
        if (!useCustomCloseTexture) {
            // 原版按钮纹理的起始位置和差值
            closeXTexStart = 0;
            closeYTexStart = 66;
            closeYDiffText = 20;
            closeTextureWidth = 256;
            closeTextureHeight = 256;
        } else {
            // 自定义图集中关闭按钮的位置 (中等按钮区域)
            closeXTexStart = 0;
            closeYTexStart = 40;
            closeYDiffText = 20;
            closeTextureWidth = 256;
            closeTextureHeight = 128;
        }
        
        this.closeHistoryButton = new ImageButton(
                this.width / 2 - 50, this.height - 30,
                closeButtonWidth, closeButtonHeight,
                closeXTexStart, closeYTexStart, closeYDiffText,
                closeButtonTexture, closeTextureWidth, closeTextureHeight,
                (button) -> toggleHistoryScreen(),
                Component.literal("-▼-")
        );

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

        ResourceLocation buttonTextureLocation = null;
        int buttonWidth = 200; // 默认值
        int buttonHeight = 20; // 默认值
        int textureActualWidth = 200; // 默认图集宽度
        int textureActualHeight = 40; // 默认图集高度

        // 优先使用本地自定义按钮图集，如果没有则使用原版纹理
        ResourceLocation customButtonAtlas = new ResourceLocation(Dialog.MODID, "textures/buttons/button_atlas.png");
        boolean useCustomTexture = false;
        
        try {
            Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(customButtonAtlas);
            if (resourceOptional.isPresent()) {
                buttonTextureLocation = customButtonAtlas;
                // 使用统一图集尺寸
                textureActualWidth = 256;
                textureActualHeight = 128;
                buttonWidth = 200;
                buttonHeight = 20;
                useCustomTexture = true;
            }
        } catch (Exception e) {
            Dialog.LOGGER.error("Error accessing custom button atlas resource: {}", customButtonAtlas, e);
        }
        
        if (!useCustomTexture) {
            // 使用Minecraft原版按钮纹理
            buttonTextureLocation = new ResourceLocation("minecraft", "textures/gui/widgets.png");
            buttonWidth = 200;
            buttonHeight = 20;
            textureActualWidth = 256;
            textureActualHeight = 256;
        }

        int buttonSpacing = 5;
        int totalHeight = options.length * (buttonHeight + buttonSpacing) - buttonSpacing;
        int startY = dialogBoxY - totalHeight - 10;


        for (int i = 0; i < options.length; i++) {
            DialogOption option = options[i];
            int buttonY = startY + i * (buttonHeight + buttonSpacing);

            // 根据是否使用原版纹理设置不同的纹理起始位置和差值
            int xTexStart = 0;
            int yTexStart = 0;
            int yDiffText = buttonHeight;
            
            if (!useCustomTexture) {
                // 原版按钮纹理的起始位置和差值
                xTexStart = 0;
                yTexStart = 66;
                yDiffText = 20; // 原版按钮的高度差值
            } else {
                // 自定义图集中选项按钮的位置 (大按钮区域)
                xTexStart = 0;
                yTexStart = 0;
                yDiffText = 20; // 悬停状态在下方20像素处
            }
            
            OptionButton button = new OptionButton(
                    (width - buttonWidth) / 2, // xPos
                    buttonY,                   // yPos
                    buttonWidth,               // width
                    buttonHeight,              // height
                    xTexStart,                 // xTexStart
                    yTexStart,                 // yTexStart
                    yDiffText,                 // yDiffText
                    buttonTextureLocation,     // resourceLocation
                    textureActualWidth,        // textureWidth
                    textureActualHeight,       // textureHeight
                    b -> {                     // onPress
                        // 执行选项指令（如果存在）
                        if (option.getCommand() != null && !option.getCommand().isEmpty()) {
                            DialogManager.getInstance().executeCommands(this.getMinecraft().player, option.getCommand());
                        }
                        DialogManager.getInstance().recordChoiceForCurrentDialog(option.getText(this.playerName).getString());
                        DialogManager.getInstance().jumpToDialog(option.getTargetId());
                    },
                    option.getText(playerName) // message
            );

            optionButtons.add(button);
            addRenderableWidget(button);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {

        // 首先渲染背景图片 (如果存在且加载成功)
        if (this.backgroundImageDisplayData != null && this.backgroundImageDisplayData.loadedSuccessfully) {
            renderBackgroundImage(guiGraphics, this.backgroundImageDisplayData);
        }

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

                    if (top.yourzi.dialog.config.ClientConfig.ENABLE_PORTRAIT_ANIMATIONS.get() && displayData.animationType != PortraitAnimationType.NONE && displayData.animationStartTime != -1) {
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

                    int finalX = baseX + (int) xOffset;
                    int finalY = baseY + (int) yOffset;

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
                ResourceLocation dialogBgRl = new ResourceLocation(Dialog.MODID, backgroundImagePath);

                RenderSystem.setShader(GameRenderer::getPositionTexShader); // 确保使用正确的着色器
                RenderSystem.setShaderTexture(0, dialogBgRl); // 绑定纹理
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // 重置颜色，确保图片不受先前渲染影响
                RenderSystem.enableBlend(); // 为透明图片启用混合
                RenderSystem.defaultBlendFunc(); // 使用默认混合函数

                // 将图片拉伸至对话框大小进行渲染
                guiGraphics.blit(dialogBgRl, dialogBoxX, dialogBoxY, 0, 0.0F, 0.0F, dialogBoxWidth, dialogBoxHeight, dialogBoxWidth, dialogBoxHeight);

                RenderSystem.disableBlend(); // 绘制完毕后禁用混合
            } catch (Exception e) {
                Dialog.LOGGER.error("Failed to render dialog background image: " + backgroundImagePath + ". Falling back to solid color.", e);
                // 回退到纯色背景
                int backgroundColor = top.yourzi.dialog.config.ClientConfig.DIALOG_BACKGROUND_COLOR.get();
        int opacity = top.yourzi.dialog.config.ClientConfig.DIALOG_BACKGROUND_OPACITY.get();
                int color = (opacity << 24) | (backgroundColor & 0xFFFFFF);
                guiGraphics.fill(dialogBoxX, dialogBoxY, dialogBoxX + dialogBoxWidth, dialogBoxY + dialogBoxHeight, color);
            }
        } else {

            int backgroundColor = top.yourzi.dialog.config.ClientConfig.DIALOG_BACKGROUND_COLOR.get();
            int opacity = top.yourzi.dialog.config.ClientConfig.DIALOG_BACKGROUND_OPACITY.get();
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
        int padding = top.yourzi.dialog.config.ClientConfig.DIALOG_BOX_PADDING.get();
        int textX = dialogBoxX + padding;
        int textY = dialogBoxY + padding;

        // 如果显示说话者名称且有说话者
        Component speakerComponent = dialogEntry.getSpeaker(playerName);
        if (top.yourzi.dialog.config.ClientConfig.SHOW_SPEAKER_NAME.get() && speakerComponent != null && !speakerComponent.getString().isEmpty()) {
            guiGraphics.drawString(font, speakerComponent, textX, textY, 0xFFFFFF);
            textY += font.lineHeight + 5;
        }

        // 渲染对话文本
        String rawText = dialogEntry.getText(playerName).getString();
        if (rawText != null && !rawText.isEmpty()) {
            int maxWidth = dialogBoxWidth - (padding * 2);
            int textAnimationSpeed = top.yourzi.dialog.config.ClientConfig.TEXT_ANIMATION_SPEED.get(); // 每秒字符数

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

            // 渲染对话中展示的物品
            if (!this.displayItemStacks.isEmpty() && textFullyDisplayed) {
                int itemSize = 16;
                int itemPadding = 4;
                int totalItemWidth = (this.displayItemStacks.size() * itemSize) + (Math.max(0, this.displayItemStacks.size() - 1) * itemPadding);

                int startX = dialogBoxX + (dialogBoxWidth - totalItemWidth) / 2;
                int itemY = dialogBoxY - itemSize - 5;

                for (ItemStack itemStack : this.displayItemStacks) {

                    guiGraphics.renderItem(itemStack, startX, itemY);

                    if (mouseX >= startX && mouseX < startX + itemSize && mouseY >= itemY && mouseY < itemY + itemSize) {
                        guiGraphics.fill(startX, itemY, startX + itemSize, itemY + itemSize, 0x80000000);
                    }

                    guiGraphics.renderItemDecorations(this.font, itemStack, startX, itemY);

                    startX += itemSize + itemPadding;
                }

                startX = dialogBoxX + (dialogBoxWidth - totalItemWidth) / 2;
                for (ItemStack itemStack : this.displayItemStacks) {
                    if (mouseX >= startX && mouseX < startX + itemSize && mouseY >= itemY && mouseY < itemY + itemSize) {
                        guiGraphics.renderTooltip(this.font, itemStack, mouseX, mouseY);
                    }
                    startX += itemSize + itemPadding;
                }
            }

            // 如果自动播放开启，且文本完全显示，且没有选项，则延迟后自动前进
            if (DialogManager.isAutoPlaying() && textFullyDisplayed && !dialogEntry.hasOptions()) {
                if (System.currentTimeMillis() - lastCharTime > top.yourzi.dialog.config.ClientConfig.AUTO_ADVANCE_DELAY.get()) { // lastCharTime 在文本完全显示后更新
                    DialogManager.getInstance().showNextDialog();
                    // 执行当前对话条目的指令
                    if (dialogEntry.getCommand() != null && !dialogEntry.getCommand().isEmpty()) {
                        DialogManager.getInstance().executeCommands(this.getMinecraft().player, dialogEntry.getCommands());
                    }
                    return;
                }
            }

            List<net.minecraft.util.FormattedCharSequence> lines;
            if (textFullyDisplayed) {
                lines = font.split(dialogEntry.getText(playerName), maxWidth);
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
                guiGraphics.drawString(font, line, textX, textY, top.yourzi.dialog.config.ClientConfig.DIALOG_TEXT_COLOR.get());
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

        // 悬浮文本提示
        if (this.viewHistoryButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("dialog.ui.history"), mouseX, mouseY);
        }
        if (this.autoPlayButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("dialog.ui.auto_play"), mouseX, mouseY);
        }

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
            // 检查服务端配置和对话条目配置是否允许跳过
            boolean serverAllowsSkip = top.yourzi.dialog.config.ServerConfig.ALLOW_SKIP_DIALOG.get();
            boolean entryAllowsSkip = dialogEntry.isSkipAllowed();
            
            if (serverAllowsSkip && entryAllowsSkip) {
                if (fastForwardCooldown > 0) {
                    fastForwardCooldown--;
                } else {
                    DialogManager.setFastForwardingNext(true);
                    DialogManager.getInstance().showNextDialog();
                    return; // 立即跳到下一条，避免渲染当前帧的剩余部分
                }
            }
        } else {
            // 如果Ctrl未按下或有选项，则清除快速跳过标记，确保正常流程
            DialogManager.setFastForwardingNext(false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return top.yourzi.dialog.config.ClientConfig.IS_PAUSE_SCREEN.get();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 首先处理ESC键的特定行为
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.showingHistory) {
                // 如果在历史记录界面，ESC键返回对话界面
                toggleHistoryScreen();
                return true; // 事件已处理
            } else {
                // 如果在对话界面，ESC键弹出确认关闭的提示
                this.minecraft.setScreen(new ConfirmScreen(
                        this::confirmCloseDialog,
                        Component.translatable("dialog.ui.esc"), // 确认框标题
                        Component.translatable("dialog.ui.confirm_esc") // 确认框消息
                ));
                return true; // 事件已处理
            }
        }

        // 处理其他键的通用行为
        if (this.showingHistory) {
            return false;
        }

        // 当文本完全显示，且没有选项时，按空格键可以手动前进
        if (textFullyDisplayed && !dialogEntry.hasOptions() && (keyCode == GLFW.GLFW_KEY_SPACE)) {
            if (DialogManager.isAutoPlaying()) {
                DialogManager.stopAutoPlay();
                updateAutoPlayButtonText();
            }
            if (dialogEntry.getCommand() != null && !dialogEntry.getCommand().isEmpty()) {
                DialogManager.getInstance().executeCommands(this.getMinecraft().player, dialogEntry.getCommands());
            }
            DialogManager.getInstance().showNextDialog();
            return true;
        }

        // 如果文本未完全显示，按空格则立即显示全部文本
        if (!textFullyDisplayed && (keyCode == GLFW.GLFW_KEY_SPACE)) {
            if (DialogManager.isAutoPlaying()) {
                DialogManager.stopAutoPlay();
                updateAutoPlayButtonText();
            }
            textFullyDisplayed = true;
            currentCharIndex = dialogEntry.getText(playerName).getString().length();
            lastCharTime = System.currentTimeMillis();
            return true;
        }

        // 对于其他未处理的按键，调用父类的处理方法
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    //处理关闭对话确认的回调方法
    private void confirmCloseDialog(boolean confirmed) {
        if (confirmed) {
            this.onClose(); // 调用Screen的onClose方法，通常是关闭屏幕
        } else {
            // 如果用户选择“否”，则重新显示当前对话界面
            if (this.minecraft != null) {
                this.minecraft.setScreen(this);
            }
        }
    }

    @Override
    public void onClose() {
        if (backgroundImageDisplayData != null && !isClosing) {
            // 开始淡出动画
            isClosing = true;
            backgroundFadeOutStartTime = System.currentTimeMillis();
            // 延迟关闭，等待淡出动画完成
            new Thread(() -> {
                try {
                    Thread.sleep(BACKGROUND_FADE_DURATION_MS);
                    minecraft.execute(() -> super.onClose());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    minecraft.execute(() -> super.onClose());
                }
            }).start();
        } else {
            super.onClose(); // 调用父类的onClose，确保屏幕正常关闭
        }
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
                    currentCharIndex = dialogEntry.getText(playerName).getString().length();
                    lastCharTime = 0; // 重置动画或自动播放的时间
                    return true; // 消费点击事件
                } else {
                    // 文本已完全显示
                    if (!dialogEntry.hasOptions()) {
                        // 如果没有选项，则推进对话
                        // 执行当前对话条目的指令（如果存在）
                        if (dialogEntry.getCommand() != null && !dialogEntry.getCommand().isEmpty()) {
                            DialogManager.getInstance().executeCommands(this.getMinecraft().player, dialogEntry.getCommands());
                        }
                        DialogManager.getInstance().showNextDialog();
                        return true; // 消费点击事件
                    }
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

        //悬浮文本提示
        if (this.closeHistoryButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("dialog.ui.close_history"), mouseX, mouseY);
        }
        
        // 渲染背景
        guiGraphics.fill(0, 0, this.width, this.height, 0xCC000000); // 半透明黑色背景

        int currentY = (int) (this.height * 0.1);
        final int textPaddingLeft = 50;
        final int optionPaddingLeft = textPaddingLeft + 5;
        final int extraEmptyLineHeight = font.lineHeight;
        final int historyAreaTopY = (int) (this.height * 0.1);
        final int historyAreaBottomY = this.height - 40; // 底部留出空间给关闭按钮等
        final int historyAreaHeight = historyAreaBottomY - historyAreaTopY;

        // 计算最大宽度
        final int dialogTextMaxWidth = Math.max(1, this.width - textPaddingLeft - 20 - 15); // 20 右缩进, 15 为滚动条宽度和间距
        final int optionTextMaxWidth = Math.max(1, this.width - optionPaddingLeft - 20 - 15);

        // 重新计算内容总高度
        totalHistoryContentHeight = 0;
        for (DialogEntry entry : historyEntries) {
            Component currentEntrySpeaker = entry.getSpeaker(playerName);
            Component dialogText = entry.getText(playerName);
            Component lineToRender;
            if (dialogText == null) dialogText = Component.empty();
            if (currentEntrySpeaker != null && !currentEntrySpeaker.getString().isEmpty()) {
                lineToRender = Component.literal("[").append(currentEntrySpeaker).append("] ").append(dialogText);
            } else {
                lineToRender = dialogText;
            }
            if (lineToRender != null) {
                List<net.minecraft.util.FormattedCharSequence> wrappedDialogLines = font.split(lineToRender, dialogTextMaxWidth);
                if (wrappedDialogLines.isEmpty() && !lineToRender.getString().isEmpty()) {
                    totalHistoryContentHeight += font.lineHeight + 2;
                } else {
                    for (net.minecraft.util.FormattedCharSequence line : wrappedDialogLines) {
                        totalHistoryContentHeight += font.lineHeight + 2;
                    }
                }
            } else {
                totalHistoryContentHeight += font.lineHeight + 2;
            }
            totalHistoryContentHeight += 5; // 条目间距
            if (entry.getSelectedOptionText() != null && !entry.getSelectedOptionText().isEmpty()) {
                Component optionComponent = Component.literal(" -> " + entry.getSelectedOptionText());
                List<net.minecraft.util.FormattedCharSequence> wrappedOptionLines = font.split(optionComponent, optionTextMaxWidth);
                if (wrappedOptionLines.isEmpty() && !optionComponent.getString().isEmpty()) {
                    totalHistoryContentHeight += font.lineHeight + 2;
                } else {
                    for (net.minecraft.util.FormattedCharSequence line : wrappedOptionLines) {
                        totalHistoryContentHeight += font.lineHeight + 2;
                    }
                }
                totalHistoryContentHeight += extraEmptyLineHeight; // 选项后间距
            }
        }

        // 渲染实际可见内容
        currentY = historyAreaTopY - historyScrollOffset; // 应用滚动偏移

        for (DialogEntry entry : historyEntries) {


            Component currentEntrySpeaker = entry.getSpeaker(playerName);
            Component dialogText = entry.getText(playerName);
            Component lineToRender;

            if (dialogText == null) {
                dialogText = Component.empty();
            }

            if (currentEntrySpeaker != null && !currentEntrySpeaker.getString().isEmpty()) {
                lineToRender = Component.literal("[").append(currentEntrySpeaker).append("] ").append(dialogText);
            } else {
                lineToRender = dialogText;
            }

            int entryStartY = currentY;
            int entryHeight = 0;

            if (lineToRender != null) {
                List<net.minecraft.util.FormattedCharSequence> wrappedDialogLines = font.split(lineToRender, dialogTextMaxWidth);
                if (wrappedDialogLines.isEmpty() && !lineToRender.getString().isEmpty()) {
                    if (currentY + font.lineHeight > historyAreaTopY && currentY < historyAreaBottomY) {
                        guiGraphics.drawString(font, lineToRender, textPaddingLeft, currentY, 0xFFFFFF);
                    }
                    currentY += font.lineHeight + 2;
                    entryHeight += font.lineHeight + 2;
                } else {
                    for (net.minecraft.util.FormattedCharSequence line : wrappedDialogLines) {
                        if (currentY + font.lineHeight > historyAreaTopY && currentY < historyAreaBottomY) {
                            guiGraphics.drawString(font, line, textPaddingLeft, currentY, 0xFFFFFF);
                        }
                        currentY += font.lineHeight + 2;
                        entryHeight += font.lineHeight + 2;
                    }
                }
            } else {
                currentY += font.lineHeight + 2;
                entryHeight += font.lineHeight + 2;
            }
            currentY += 5;
            entryHeight += 5;

            // 显示选择的选项
            if (entry.getSelectedOptionText() != null && !entry.getSelectedOptionText().isEmpty()) {
                Component optionComponent = Component.literal(" -> " + entry.getSelectedOptionText());
                currentY += 5;
                entryHeight += 5;

                List<net.minecraft.util.FormattedCharSequence> wrappedOptionLines = font.split(optionComponent, optionTextMaxWidth);
                if (wrappedOptionLines.isEmpty() && !optionComponent.getString().isEmpty()) {
                    if (currentY + font.lineHeight > historyAreaTopY && currentY < historyAreaBottomY) {
                        guiGraphics.drawString(font, optionComponent, optionPaddingLeft, currentY, 0xAAAAAA);
                    }
                    currentY += font.lineHeight + 2;
                    entryHeight += font.lineHeight + 2;
                } else {
                    for (net.minecraft.util.FormattedCharSequence line : wrappedOptionLines) {
                        if (currentY + font.lineHeight > historyAreaTopY && currentY < historyAreaBottomY) {
                            guiGraphics.drawString(font, line, optionPaddingLeft, currentY, 0xAAAAAA);
                        }
                        currentY += font.lineHeight + 2;
                        entryHeight += font.lineHeight + 2;
                    }
                }
                currentY += extraEmptyLineHeight;
                entryHeight += extraEmptyLineHeight;
            }
            // 如果条目的任何部分在可视区域之上，并且其结束部分在可视区域之下，则认为该条目是（部分）可见的
        }

        // 更新滚动状态
        canScrollHistoryUp = historyScrollOffset > 0;
        canScrollHistoryDown = totalHistoryContentHeight > historyAreaHeight && historyScrollOffset < (totalHistoryContentHeight - historyAreaHeight);

        // 渲染滚动提示箭头 (向下)
        if (canScrollHistoryDown) {
            int arrowX = this.width / 2;
            int arrowY = historyAreaBottomY + 5; // 在历史区域下方
            guiGraphics.drawString(font, "▼", arrowX - font.width("▼") / 2, arrowY, 0xFFFFFF);
        }
        // 渲染滚动提示箭头 (向上)
        if (canScrollHistoryUp) {
            int arrowX = this.width / 2;
            int arrowY = historyAreaTopY - font.lineHeight - 5; // 在历史区域上方
            guiGraphics.drawString(font, "▲", arrowX - font.width("▲") / 2, arrowY, 0xFFFFFF);
        }

        // 渲染滚动条
        if (totalHistoryContentHeight > historyAreaHeight) {
            int scrollbarWidth = 5;
            int scrollbarX = this.width - textPaddingLeft + 20; // 调整到文本区域右侧
            int scrollbarTrackHeight = historyAreaHeight;

            // 滚动条背景
            guiGraphics.fill(scrollbarX, historyAreaTopY, scrollbarX + scrollbarWidth, historyAreaTopY + scrollbarTrackHeight, 0xFF555555);

            float scrollPercentage = (float) historyScrollOffset / (totalHistoryContentHeight - historyAreaHeight);
            int scrollThumbHeight = Math.max(20, (int) ((float) historyAreaHeight / totalHistoryContentHeight * historyAreaHeight));
            int scrollThumbY = historyAreaTopY + (int) (scrollPercentage * (scrollbarTrackHeight - scrollThumbHeight));

            guiGraphics.fill(scrollbarX, scrollThumbY, scrollbarX + scrollbarWidth, scrollThumbY + scrollThumbHeight, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.showingHistory) {
            int scrollAmount = (int) (-delta * (font.lineHeight + 2) * 2); // 每次滚动2行的高度
            int newScrollOffset = this.historyScrollOffset + scrollAmount;
            int maxScroll = Math.max(0, totalHistoryContentHeight - (this.height - 40 - (int) (this.height * 0.1)));

            this.historyScrollOffset = Mth.clamp(newScrollOffset, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    //存储每个立绘的显示数据
    private static class PortraitDisplayData {
        private final static HashMap<ResourceLocation, BufferedImage> CACHED = new HashMap<>();
        ResourceLocation resourceLocation;
        int actualWidth;
        int actualHeight;
        float brightness = 1.0f;
        PortraitPosition position;
        PortraitAnimationType animationType = PortraitAnimationType.NONE;
        long animationStartTime = -1;
        boolean loadedSuccessfully = false;

        public static void clearCache() {
            CACHED.clear();
            Dialog.LOGGER.info("Portrait cache cleared.");
        }

        PortraitDisplayData(String path, float brightness, PortraitPosition position, PortraitAnimationType animationType) {
            if (path != null && !path.isEmpty()) {
                this.resourceLocation = new ResourceLocation(Dialog.MODID, String.format("textures/portraits/%s", path));
                this.brightness = brightness;
                this.position = position != null ? position : PortraitPosition.RIGHT; // 位置
                this.animationType = animationType != null ? animationType : PortraitAnimationType.NONE; // 动画类型
                loadDimensions();
                if (top.yourzi.dialog.config.ClientConfig.ENABLE_PORTRAIT_ANIMATIONS.get() && loadedSuccessfully && this.animationType != PortraitAnimationType.NONE) {
                    this.animationStartTime = System.currentTimeMillis();
                }
            } else {
                Dialog.LOGGER.warn("Portrait path is null or empty. Cannot load portrait.");
            }
        }

        private void loadDimensions() {
            if (this.resourceLocation == null) return;
            var target_bufferedimage = CACHED.get(this.resourceLocation);
            if (target_bufferedimage == null) {
                try {
                    Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(this.resourceLocation);
                    if (resourceOptional.isPresent()) {
                        try (final var inputStream = resourceOptional.get().open()) {
                            target_bufferedimage = STBBackendImage.read(inputStream);
                            this.actualWidth = target_bufferedimage.getWidth();
                            this.actualHeight = target_bufferedimage.getHeight();
                            this.loadedSuccessfully = true;
                            CACHED.put(this.resourceLocation, target_bufferedimage);
                        }
                    } else {
                        Dialog.LOGGER.warn("Portrait resource not found: {}.", this.resourceLocation);
                    }
                } catch (IOException e) {
                    Dialog.LOGGER.error("Error reading portrait image {}: {}.", this.resourceLocation, e.getMessage());
                } catch (Exception e) {
                    Dialog.LOGGER.error("Unexpected error loading portrait image {}: {}.", this.resourceLocation, e.getMessage());
                }
            } else {
                this.actualWidth = target_bufferedimage.getWidth();
                this.actualHeight = target_bufferedimage.getHeight();
                this.loadedSuccessfully = true;
            }
        }
    }


    private void renderBackgroundImage(GuiGraphics guiGraphics, BackgroundImageDisplayData bgData) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, bgData.imageLocation);
        
        // 计算基于动画类型的透明度
        float alpha = 1.0F;
        
        // 优先处理关闭时的淡出效果
        if (isClosing && backgroundFadeOutStartTime > 0) {
            // 淡出阶段：从1到0
            long elapsedTime = System.currentTimeMillis() - backgroundFadeOutStartTime;
            if (elapsedTime < BACKGROUND_FADE_DURATION_MS) {
                alpha = Math.max(0.0F, 1.0F - (float) elapsedTime / BACKGROUND_FADE_DURATION_MS);
            } else {
                alpha = 0.0F;
            }
        } else {
            // 根据动画类型计算透明度
            switch (bgData.animationType) {
                case FADE_IN:
                    if (bgData.animationStartTime > 0) {
                        long elapsedTime = System.currentTimeMillis() - bgData.animationStartTime;
                        if (elapsedTime < BACKGROUND_FADE_DURATION_MS) {
                            alpha = Math.min(1.0F, (float) elapsedTime / BACKGROUND_FADE_DURATION_MS);
                        }
                    }
                    break;
                case NONE:
                default:
                    alpha = 1.0F;
                    break;
            }
        }
        
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int screenWidth = this.width;
        int screenHeight = this.height;
        int imgWidth = bgData.imageWidth;
        int imgHeight = bgData.imageHeight;

        BackgroundRenderOption renderOption = bgData.renderOption!= null? bgData.renderOption : BackgroundRenderOption.FILL;

        switch (renderOption) {
            case FILL:
                float screenAspect = (float) screenWidth / screenHeight;
                float imageAspect = (float) imgWidth / imgHeight;
                int drawWidth, drawHeight, drawX, drawY;
                if (imageAspect > screenAspect) {
                    drawHeight = screenHeight;
                    drawWidth = (int) (screenHeight * imageAspect);
                    drawX = (screenWidth - drawWidth) / 2;
                    drawY = 0;
                } else {
                    drawWidth = screenWidth;
                    drawHeight = (int) (screenWidth / imageAspect);
                    drawX = 0;
                    drawY = (screenHeight - drawHeight) / 2;
                }
                guiGraphics.blit(bgData.imageLocation, drawX, drawY, drawWidth, drawHeight, 0, 0, imgWidth, imgHeight, imgWidth, imgHeight);
                break;
            case FIT:
                screenAspect = (float) screenWidth / screenHeight;
                imageAspect = (float) imgWidth / imgHeight;
                if (imageAspect > screenAspect) {
                    drawWidth = screenWidth;
                    drawHeight = (int) (screenWidth / imageAspect);
                } else {
                    drawHeight = screenHeight;
                    drawWidth = (int) (screenHeight * imageAspect);
                }
                drawX = (screenWidth - drawWidth) / 2;
                drawY = (screenHeight - drawHeight) / 2;
                guiGraphics.blit(bgData.imageLocation, drawX, drawY, drawWidth, drawHeight, 0, 0, imgWidth, imgHeight, imgWidth, imgHeight);
                break;
            case STRETCH:
                guiGraphics.blit(bgData.imageLocation, 0, 0, screenWidth, screenHeight, 0, 0, imgWidth, imgHeight, imgWidth, imgHeight);
                break;
            case TILE:
                for (int y = 0; y < screenHeight; y += imgHeight) {
                    for (int x = 0; x < screenWidth; x += imgWidth) {
                        int w = Math.min(imgWidth, screenWidth - x);
                        int h = Math.min(imgHeight, screenHeight - y);
                        guiGraphics.blit(bgData.imageLocation, x, y, 0, 0, w, h, imgWidth, imgHeight);
                    }
                }
                break;
            case CENTER:
                drawX = (screenWidth - imgWidth) / 2;
                drawY = (screenHeight - imgHeight) / 2;
                guiGraphics.blit(bgData.imageLocation, drawX, drawY, imgWidth, imgHeight, 0, 0, imgWidth, imgHeight, imgWidth, imgHeight);
                break;
        }
        RenderSystem.disableBlend();
    }
}