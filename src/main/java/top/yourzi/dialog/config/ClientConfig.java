package top.yourzi.dialog.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import top.yourzi.dialog.Dialog;

/**
 * 对话系统的客户端配置类。
 */
@Mod.EventBusSubscriber(modid = Dialog.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    
    // 对话框UI配置
    public static final ForgeConfigSpec.ConfigValue<Integer> DIALOG_BOX_WIDTH; // 对话框宽度
    public static final ForgeConfigSpec.ConfigValue<Integer> DIALOG_BOX_HEIGHT; // 对话框高度
    public static final ForgeConfigSpec.ConfigValue<Integer> DIALOG_BOX_PADDING; // 对话框内边距
    public static final ForgeConfigSpec.ConfigValue<Integer> DIALOG_TEXT_COLOR; // 对话文本默认颜色
    public static final ForgeConfigSpec.ConfigValue<Integer> DIALOG_BACKGROUND_COLOR; // 对话框背景颜色
    public static final ForgeConfigSpec.ConfigValue<Integer> DIALOG_BACKGROUND_OPACITY; // 对话框背景不透明度
    
    // 立绘配置
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_PORTRAIT_ANIMATIONS; // 启用立绘动画
    
    // 对话系统配置
    public static final ForgeConfigSpec.ConfigValue<Boolean> IS_PAUSE_SCREEN; // 是否在对话时暂停游戏（仅单人）
    public static final ForgeConfigSpec.ConfigValue<Integer> AUTO_ADVANCE_DELAY; // 自动推进对话延迟 (毫秒)
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOW_SPEAKER_NAME; // 显示说话者名称
    public static final ForgeConfigSpec.ConfigValue<Integer> TEXT_ANIMATION_SPEED; // 文本逐字显示速度 (每秒字符数，0表示立即显示全部)
    
    static {
        BUILDER.comment("对话系统客户端配置").push("dialog_client");

        BUILDER.comment("对话框UI配置").push("ui");
        DIALOG_BOX_WIDTH = BUILDER
                .comment("对话框宽度")
                .define("dialogBoxWidth", 320);
        DIALOG_BOX_HEIGHT = BUILDER
                .comment("对话框高度")
                .define("dialogBoxHeight", 100);
        DIALOG_BOX_PADDING = BUILDER
                .comment("对话框内边距")
                .define("dialogBoxPadding", 10);
        DIALOG_TEXT_COLOR = BUILDER
                .comment("对话文本默认颜色 (ARGB格式)")
                .define("dialogTextColor", 0xFFFFFFFF);
        DIALOG_BACKGROUND_COLOR = BUILDER
                .comment("对话框背景默认颜色 (RGB格式)")
                .define("dialogBackgroundColor", 0x000000);
        DIALOG_BACKGROUND_OPACITY = BUILDER
                .comment("对话框背景不透明度 (0-255)")
                .define("dialogBackgroundOpacity", 200);
        BUILDER.pop();

        BUILDER.comment("立绘配置").push("portrait");
        ENABLE_PORTRAIT_ANIMATIONS = BUILDER
                .comment("启用立绘动画")
                .define("enablePortraitAnimations", true);
        BUILDER.pop();

        BUILDER.comment("对话系统配置").push("system");
        IS_PAUSE_SCREEN = BUILDER
                .comment("是否在对话时暂停游戏（仅单人模式）")
                .define("isPauseScreen", false);
        AUTO_ADVANCE_DELAY = BUILDER
                .comment("自动推进对话的延迟时间（毫秒）")
                .define("autoAdvanceDelay", 200);
        SHOW_SPEAKER_NAME = BUILDER
                .comment("是否显示说话者的名称")
                .define("showSpeakerName", true);
        TEXT_ANIMATION_SPEED = BUILDER
                .comment("文本逐字显示的速度（每秒字符数，设置为0则立即显示全部文本）")
                .defineInRange("textAnimationSpeed", 20, 0, 1000);
        BUILDER.pop();

        BUILDER.pop();
    }
    
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    }
}