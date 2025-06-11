package top.yourzi.dialog.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import top.yourzi.dialog.Dialog;

/**
 * 对话系统的服务端配置类。
 */
@Mod.EventBusSubscriber(modid = Dialog.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    
    // 对话系统服务端配置
    public static final ForgeConfigSpec.ConfigValue<Boolean> ALLOW_SKIP_DIALOG; // 是否允许跳过对话
    
    static {
        BUILDER.comment("对话系统服务端配置").push("dialog_server");

        BUILDER.comment("对话控制配置").push("control");
        ALLOW_SKIP_DIALOG = BUILDER
                .comment("是否允许玩家使用Ctrl键跳过对话")
                .define("allowSkipDialog", true);
        BUILDER.pop();

        BUILDER.pop();
    }
    
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    }
}