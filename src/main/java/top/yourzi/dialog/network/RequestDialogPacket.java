package top.yourzi.dialog.network;

import com.google.gson.Gson;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.yourzi.dialog.Dialog;
import top.yourzi.dialog.DialogManager;
import top.yourzi.dialog.model.DialogSequence;

/**
 * 客户端向服务端请求特定对话数据的网络包 (C2S).
 */
public record RequestDialogPacket(String dialogId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestDialogPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dialog.MODID, "request_dialog_packet"));

    public RequestDialogPacket(String dialogId) {
        this.dialogId = dialogId;
    }

    public static final StreamCodec<FriendlyByteBuf, RequestDialogPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        RequestDialogPacket::dialogId,
        RequestDialogPacket::new
    );

    public String getDialogId() {
        return dialogId;
    }

    /**
     * 处理接收到的包 (在服务端执行)。
     */
    public static void handleServer(final RequestDialogPacket message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer sender = (ServerPlayer) context.player();
            if (sender != null) {
                DialogManager dialogManager = DialogManager.getInstance();
                DialogSequence sequence = dialogManager.getDialogSequence(message.dialogId);
                if (sequence != null) {
                    // 将DialogSequence序列化为JSON字符串
                    Gson gson = new Gson();
                    String dialogJson = gson.toJson(sequence);
                    // 发送包含对话数据的包回客户端
                    NetworkHandler.sendDialogDataToPlayer(sender, message.dialogId, dialogJson);
                } else {
                    top.yourzi.dialog.Dialog.LOGGER.warn("Player {} requested dialog '{}' which was not found on the server.", sender.getName().getString(), message.dialogId);
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
       return TYPE;
    }
}