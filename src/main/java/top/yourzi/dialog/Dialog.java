package top.yourzi.dialog;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;


@Mod(Dialog.MODID)
public class Dialog {
    public static final String MODID = "dialog";
    public static final Logger LOGGER = LogUtils.getLogger();


    
    public Dialog() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置项
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 注册事件监听器
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerKeyBindings);

        // 注册 MinecraftForge 事件总线
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("对话框模组通用设置初始化");

        // 初始化网络处理器
        event.enqueueWork(top.yourzi.dialog.network.NetworkHandler::init);
    }
    
    private void onClientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("对话框模组客户端设置初始化");

        // 客户端设置完成后加载对话数据
        event.enqueueWork(() -> DialogManager.getInstance().loadDialogs(Minecraft.getInstance().getResourceManager(), true));
    }
    
    private void registerKeyBindings(final RegisterKeyMappingsEvent event) {
        // 注册按键绑定（如果需要）
    }
}
