package top.yourzi.dialog;

import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.yourzi.dialog.network.NetworkHandler;

import java.beans.EventHandler;

import org.slf4j.Logger;


@Mod(Dialog.MODID)
public class Dialog {
    public static final String MODID = "dialog";
    public static final Logger LOGGER = LogUtils.getLogger();


    
    public Dialog(IEventBus modEventBus, ModContainer modContainer) {

        // 注册配置项
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 注册网络包处理器
        modEventBus.addListener(NetworkHandler::register);

    }
}
