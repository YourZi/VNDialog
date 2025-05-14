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
    private final String dialogJson; // 新增字段，用于存储完整的对话JSON
    
    public ShowDialogPacket(String dialogId, String dialogJson) {
        this.dialogId = dialogId;
        this.dialogJson = dialogJson;
    }

    public ShowDialogPacket(String dialogId) { // 保留旧的构造函数以便兼容，但理想情况下应移除或标记为弃用
        this(dialogId, ""); // 或者根据实际情况处理
        // Dialog.LOGGER.warn("ShowDialogPacket created with only dialogId. This might be an outdated call.");
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
                // 如果有完整的JSON数据，则使用新的处理方法
                DialogManager.getInstance().receiveAndShowPlayerSpecificDialog(this.dialogId, this.dialogJson);
            } else {
                // 兼容旧的逻辑，或者提示错误
                // Dialog.LOGGER.warn("Received ShowDialogPacket with empty dialogJson for ID: {}. Falling back to old behavior or error.", this.dialogId);
                // Fallback to old method if needed, or handle as an error.
                // For now, let's assume new method is preferred if json is present.
                // If json is truly empty, it might mean a request for a dialog that doesn't exist or an error.
                // DialogManager.getInstance().showDialog(dialogId); // 旧的调用方式
                // 考虑到新的流程，如果dialogJson为空，可能表示服务端未能生成特定于玩家的对话，
                // 此时客户端可能需要请求原始对话或显示错误。
                // 为了安全起见，如果dialogJson为空，我们暂时不执行任何操作，或记录一个更明显的警告/错误。
                top.yourzi.dialog.Dialog.LOGGER.warn("ShowDialogPacket received for id '{}' but dialogJson is empty. Client will not show dialog via this packet.", this.dialogId);
            }
        });
    }
}