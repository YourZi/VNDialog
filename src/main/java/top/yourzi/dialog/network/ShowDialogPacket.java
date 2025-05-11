package top.yourzi.dialog.network;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import top.yourzi.dialog.Dialog;
import top.yourzi.dialog.DialogManager;

/**
 * 显示对话的网络包
 */
public class ShowDialogPacket {
    private final String dialogId;
    
    public ShowDialogPacket(String dialogId) {
        this.dialogId = dialogId;
    }
    
    /**
     * 将包数据编码到字节缓冲区
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dialogId);
    }
    
    /**
     * 从字节缓冲区解码包数据
     */
    public static ShowDialogPacket decode(FriendlyByteBuf buf) {
        return new ShowDialogPacket(buf.readUtf());
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
        Dialog.LOGGER.info("收到显示对话的网络包: {}", dialogId);
        // 在客户端显示对话
        Minecraft.getInstance().execute(() -> {
            DialogManager.getInstance().showDialog(dialogId);
        });
    }
}