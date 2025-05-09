package top.yourzi.dialog.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import top.yourzi.dialog.Dialog;

import java.util.List;
import java.util.Optional;

/**
 * 网络包处理器，用于服务端和客户端之间的通信
 */
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Dialog.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /**
     * 初始化网络包处理器
     */
    public static void init() {
        // 注册从服务器到客户端的对话显示包
        INSTANCE.registerMessage(0, ShowDialogPacket.class,
                ShowDialogPacket::encode,
                ShowDialogPacket::decode,
                ShowDialogPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
                
        // 注册从服务器到客户端的重新加载对话包
        INSTANCE.registerMessage(1, ReloadDialogsPacket.class,
                ReloadDialogsPacket::encode,
                ReloadDialogsPacket::decode,
                ReloadDialogsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
                
        // 注册从服务器到客户端的对话列表包
        INSTANCE.registerMessage(2, ListDialogsPacket.class,
                ListDialogsPacket::encode,
                ListDialogsPacket::decode,
                ListDialogsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        Dialog.LOGGER.info("网络包处理器初始化完成");
    }

    /**
     * 向指定玩家发送显示对话的网络包
     */
    public static void sendShowDialogToPlayer(ServerPlayer player, String dialogId) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new ShowDialogPacket(dialogId));
    }
    
    /**
     * 向指定玩家发送重新加载对话的网络包
     */
    public static void sendReloadDialogsToPlayer(ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new ReloadDialogsPacket());
    }
    
    /**
     * 向所有玩家发送重新加载对话的网络包
     */
    public static void sendReloadDialogsToAll() {
        INSTANCE.send(PacketDistributor.ALL.noArg(), new ReloadDialogsPacket());
    }
    
    /**
     * 向指定玩家发送对话列表的网络包
     */
    public static void sendDialogListToPlayer(ServerPlayer player, List<String> dialogIds, List<String> dialogNames) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new ListDialogsPacket(dialogIds, dialogNames));
    }
}