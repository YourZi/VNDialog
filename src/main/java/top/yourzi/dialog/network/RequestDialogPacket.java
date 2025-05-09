package top.yourzi.dialog.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import top.yourzi.dialog.DialogManager;
import top.yourzi.dialog.model.DialogSequence;
import com.google.gson.Gson;

import java.util.function.Supplier;

/**
 * 客户端向服务端请求特定对话数据的网络包。
 */
public class RequestDialogPacket {
    private final String dialogId;

    public RequestDialogPacket(String dialogId) {
        this.dialogId = dialogId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.dialogId);
    }

    public static RequestDialogPacket decode(FriendlyByteBuf buf) {
        return new RequestDialogPacket(buf.readUtf());
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                DialogManager dialogManager = DialogManager.getInstance();
                DialogSequence sequence = dialogManager.getDialogSequence(this.dialogId);
                if (sequence != null) {
                    // 将DialogSequence序列化为JSON字符串
                    Gson gson = new Gson(); // Ideally, use a shared Gson instance
                    String dialogJson = gson.toJson(sequence);
                    // 发送包含对话数据的包回客户端
                    NetworkHandler.sendDialogDataToPlayer(sender, this.dialogId, dialogJson);
                } else {
                    // Optionally, send a response indicating the dialog was not found on the server
                    // For now, we'll just log it.
                    top.yourzi.dialog.Dialog.LOGGER.warn("Player {} requested dialog '{}' which was not found on the server.", sender.getName().getString(), this.dialogId);
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}