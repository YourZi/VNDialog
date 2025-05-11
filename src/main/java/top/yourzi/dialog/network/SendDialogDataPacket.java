package top.yourzi.dialog.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import top.yourzi.dialog.DialogManager;
import java.util.function.Supplier;

/**
 * 服务端向客户端发送特定对话数据的网络包。
 */
public class SendDialogDataPacket {
    private final String dialogId;
    private final String dialogJson;

    public SendDialogDataPacket(String dialogId, String dialogJson) {
        this.dialogId = dialogId;
        this.dialogJson = dialogJson;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.dialogId);
        buf.writeUtf(this.dialogJson);
    }

    public static SendDialogDataPacket decode(FriendlyByteBuf buf) {
        return new SendDialogDataPacket(buf.readUtf(), buf.readUtf());
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 确保在客户端线程中执行
            handleOnClient();
        });
        ctx.get().setPacketHandled(true);
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private void handleOnClient() {
        DialogManager.getInstance().receiveDialogData(this.dialogId, this.dialogJson);
    }
}