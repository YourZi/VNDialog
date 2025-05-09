package top.yourzi.dialog.network;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import top.yourzi.dialog.Dialog;
import top.yourzi.dialog.DialogManager;

/**
 * 重新加载对话的网络包
 */
public class ReloadDialogsPacket {
    
    public ReloadDialogsPacket() {
        // 空构造函数
    }
    
    /**
     * 将包数据编码到字节缓冲区
     */
    public void encode(FriendlyByteBuf buf) {
        // 这个包不需要传递额外数据
    }
    
    /**
     * 从字节缓冲区解码包数据
     */
    public static ReloadDialogsPacket decode(FriendlyByteBuf buf) {
        return new ReloadDialogsPacket();
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
        Dialog.LOGGER.info("收到重新加载对话的网络包");
        // 在客户端重新加载对话
        Minecraft.getInstance().execute(() -> {
            DialogManager.getInstance().loadDialogsFromServer(Minecraft.getInstance().getResourceManager());
            Dialog.LOGGER.info("客户端对话已重新加载");
        });
    }
}