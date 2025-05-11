package top.yourzi.dialog.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import top.yourzi.dialog.DialogManager;

import java.util.function.Supplier;

/**
 * 客户端向服务端发送提交物品请求的网络包。
 */
public class SubmitItemPacket {
    private final String dialogId;
    private final String itemId;
    private final String itemNbt; // 可选
    private final int itemCount;    // 可选

    public SubmitItemPacket(String dialogId, String itemId, String itemNbt, int itemCount) {
        this.dialogId = dialogId;
        this.itemId = itemId;
        this.itemNbt = itemNbt;
        this.itemCount = itemCount;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.dialogId);
        buf.writeUtf(this.itemId);
        buf.writeBoolean(this.itemNbt != null);
        if (this.itemNbt != null) {
            buf.writeUtf(this.itemNbt);
        }
        buf.writeInt(this.itemCount);
    }

    public static SubmitItemPacket decode(FriendlyByteBuf buf) {
        String dialogId = buf.readUtf();
        String itemId = buf.readUtf();
        String itemNbt = null;
        if (buf.readBoolean()) {
            itemNbt = buf.readUtf();
        }
        int itemCount = buf.readInt();
        return new SubmitItemPacket(dialogId, itemId, itemNbt, itemCount);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                // 服务端处理物品提交逻辑
                DialogManager.getInstance().handleSubmitItem(sender, this.dialogId, this.itemId, this.itemNbt, this.itemCount);
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}