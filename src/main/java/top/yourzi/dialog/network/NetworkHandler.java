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
import net.minecraft.client.Minecraft;

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
        
        // 注册从客户端到服务端的请求对话包
        INSTANCE.registerMessage(3, RequestDialogPacket.class,
                RequestDialogPacket::encode,
                RequestDialogPacket::decode,
                RequestDialogPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // 注册从服务端到客户端的发送对话数据包
        INSTANCE.registerMessage(4, SendDialogDataPacket.class,
                SendDialogDataPacket::encode,
                SendDialogDataPacket::decode,
                SendDialogDataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // 注册从服务端到客户端的同步所有对话数据包
        INSTANCE.registerMessage(5, SyncAllDialogsPacket.class, 
                SyncAllDialogsPacket::encode,
                SyncAllDialogsPacket::decode,
                SyncAllDialogsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
                
        // 注册从客户端到服务端的提交物品包
        INSTANCE.registerMessage(7, SubmitItemPacket.class, // 新的ID
                SubmitItemPacket::encode,
                SubmitItemPacket::decode,
                SubmitItemPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
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

    /**
     * 客户端向服务端发送请求特定对话数据的网络包
     */
    public static void sendRequestDialogToServer(String dialogId) {
        if (Minecraft.getInstance() != null && Minecraft.getInstance().getConnection() != null) {
            INSTANCE.sendToServer(new RequestDialogPacket(dialogId));
        } else {
            Dialog.LOGGER.warn("Cannot send RequestDialogPacket: not on client or no connection.");
        }
    }

    /**
     * 服务端向指定玩家发送特定对话数据的网络包
     */
    public static void sendDialogDataToPlayer(ServerPlayer player, String dialogId, String dialogJson) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SendDialogDataPacket(dialogId, dialogJson));
    }

    /**
     * 服务端向指定玩家发送所有对话数据的网络包。
     */
    public static void sendAllDialogsToPlayer(ServerPlayer player, java.util.Map<String, String> dialogDataMap) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncAllDialogsPacket(dialogDataMap));
    }

    /**
     * 服务端向所有玩家发送所有对话数据的网络包。
     */
    public static void sendAllDialogsToAllPlayers(java.util.Map<String, String> dialogDataMap) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncAllDialogsPacket(dialogDataMap));
    }

    /**
     * 客户端向服务端发送提交物品请求的网络包
     */
    public static void sendSubmitItemToServer(String dialogId, String itemId, String itemNbt, int itemCount) {
        if (Minecraft.getInstance() != null && Minecraft.getInstance().getConnection() != null) {
<<<<<<< HEAD
            INSTANCE.sendToServer(new SubmitItemPacket(dialogId, itemId, itemNbt, itemCount));
            Dialog.LOGGER.info("Sent request to submit item for dialog '{}': item '{}', nbt '{}', count {}", dialogId, itemId, itemNbt, itemCount);
=======
            INSTANCE.sendToServer(new ExecuteServerCommandPacket(command));
>>>>>>> 270cb6d (移除冗余的日志输出)
        } else {
            Dialog.LOGGER.warn("Cannot send SubmitItemPacket: not on client or no connection.");
        }
    }
}