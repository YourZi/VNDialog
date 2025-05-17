package top.yourzi.dialog.network;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import top.yourzi.dialog.Dialog;

public class NetworkHandler {

    public static void register(final RegisterPayloadHandlersEvent event) {
         // 设置当前网络版本
        final PayloadRegistrar registrar = event.registrar("1").executesOn(HandlerThread.NETWORK);;

        // 注册 ExecuteServerCommandPacket (C2S)
        registrar.playToServer(
            ExecuteServerCommandPacket.TYPE,
            ExecuteServerCommandPacket.STREAM_CODEC,
            ExecuteServerCommandPacket::handleServer
        );

        // 注册 ReloadDialogsPacket (S2C)
        registrar.playToClient(
            ReloadDialogsPacket.TYPE,
            ReloadDialogsPacket.STREAM_CODEC,
            ReloadDialogsPacket::handleClient
        );

        // 注册 ListDialogsPacket (S2C)
        registrar.playToClient(
            ListDialogsPacket.TYPE,
            ListDialogsPacket.STREAM_CODEC,
            ListDialogsPacket::handleClient
        );

        // 注册 ShowDialogPacket (S2C)
        registrar.playToClient(
            ShowDialogPacket.TYPE,
            ShowDialogPacket.STREAM_CODEC,
            ShowDialogPacket::handleClient
        );

        // 注册 SendDialogDataPacket (S2C)
        registrar.playToClient(
            SendDialogDataPacket.TYPE,
            SendDialogDataPacket.STREAM_CODEC,
            SendDialogDataPacket::handleClient
        );
        // 注册 SyncAllDialogsPacket (S2C)
        registrar.playToClient(
            SyncAllDialogsPacket.TYPE,
            SyncAllDialogsPacket.STREAM_CODEC,
            SyncAllDialogsPacket::handleClient
        );

        // 注册 RequestDialogPacket (C2S)
        registrar.playToServer(
            RequestDialogPacket.TYPE,
            RequestDialogPacket.STREAM_CODEC,
            RequestDialogPacket::handleServer
        );
    }

    /**
     * 向指定玩家发送显示对话的网络包
     */
    public static void sendShowDialogToPlayer(ServerPlayer player, String dialogId, String dialogJson) {
        PacketDistributor.sendToPlayer(player, new ShowDialogPacket(dialogId, dialogJson));
    }
    
    /**
     * 向指定玩家发送重新加载对话的网络包
     */
    public static void sendReloadDialogsToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ReloadDialogsPacket());
    }
    
    /**
     * 向所有玩家发送重新加载对话的网络包
     */
    public static void sendReloadDialogsToAll() {
        PacketDistributor.sendToAllPlayers(new ReloadDialogsPacket());
    }
    
    /**
     * 向指定玩家发送对话列表的网络包
     */
    public static void sendDialogListToPlayer(ServerPlayer player, List<String> dialogIds, List<String> dialogNames) {
        PacketDistributor.sendToPlayer(player, new ListDialogsPacket(dialogIds, dialogNames));
    }

    /**
     * 客户端向服务端发送请求特定对话数据的网络包
     */
    public static void sendRequestDialogToServer(String dialogId) {
        if (Minecraft.getInstance() != null && Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new RequestDialogPacket(dialogId));
        } else {
            Dialog.LOGGER.warn("Cannot send RequestDialogPacket: not on client or no connection.");
        }
    }

    /**
     * 服务端向指定玩家发送特定对话数据的网络包
     */
    public static void sendDialogDataToPlayer(ServerPlayer player, String dialogId, String dialogJson) {
        PacketDistributor.sendToPlayer(player, new SendDialogDataPacket(dialogId, dialogJson));
    }

    /**
     * 服务端向指定玩家发送所有对话数据的网络包。
     */
    public static void sendAllDialogsToPlayer(ServerPlayer player, java.util.Map<String, String> dialogDataMap) {
        PacketDistributor.sendToAllPlayers(new SyncAllDialogsPacket(dialogDataMap));
    }

    /**
     * 服务端向所有玩家发送所有对话数据的网络包。
     */
    public static void sendAllDialogsToAllPlayers(java.util.Map<String, String> dialogDataMap) {
        PacketDistributor.sendToAllPlayers(new SyncAllDialogsPacket(dialogDataMap));
    }

    /**
     * 客户端向服务端发送执行命令请求的网络包
     */
    public static void sendExecuteCommandToServer(String command) {
        if (Minecraft.getInstance() != null && Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new ExecuteServerCommandPacket(command));
        } else {
            Dialog.LOGGER.warn("Cannot send ExecuteServerCommandPacket: not on client or no connection.");
        }
    }
}