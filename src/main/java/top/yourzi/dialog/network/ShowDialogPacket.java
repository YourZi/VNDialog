package top.yourzi.dialog.network;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import top.yourzi.dialog.DialogManager;

/**
 * 显示对话的网络包
 */
public class ShowDialogPacket {
    private final String dialogId;
    private final String dialogJson;
    
    public ShowDialogPacket(String dialogId, String dialogJson) {
        this.dialogId = dialogId;
        this.dialogJson = dialogJson;
    }

    public ShowDialogPacket(String dialogId) {
        this(dialogId, "");
    }

    /**
     * 将包数据编码到字节缓冲区
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dialogId);
        buf.writeUtf(dialogJson); // 编码新增的字段
    }

    /**
     * 从字节缓冲区解码包数据
     */
    public static ShowDialogPacket decode(FriendlyByteBuf buf) {
        String dialogId = buf.readUtf();
        String dialogJson = buf.readUtf(); // 解码新增的字段
        return new ShowDialogPacket(dialogId, dialogJson);
    }

    /**
     * 处理接收到的包
     */
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 确保在客户端线程中执行
            handleOnClient();
        });
        ctx.get().setPacketHandled(true);
        return true;
    }

    /**
     * 在客户端处理包
     */
    @OnlyIn(Dist.CLIENT)
    private void handleOnClient() {
        // 在客户端显示对话
        Minecraft.getInstance().execute(() -> {
            if (this.dialogJson != null && !this.dialogJson.isEmpty()) {
                DialogManager.getInstance().receiveAndShowPlayerSpecificDialog(this.dialogId, this.dialogJson);
            } else {
                top.yourzi.dialog.Dialog.LOGGER.warn("ShowDialogPacket received for id '{}' but dialogJson is empty. Client will not show dialog via this packet.", this.dialogId);
            }
        });
    }
}