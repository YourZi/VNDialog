package top.yourzi.dialog.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import top.yourzi.dialog.Dialog;
import top.yourzi.dialog.DialogManager;

/**
 * 列出所有对话的网络包
 */
public class ListDialogsPacket {
    private final List<String> dialogIds;
    private final List<String> dialogNames;
    
    public ListDialogsPacket(List<String> dialogIds, List<String> dialogNames) {
        this.dialogIds = dialogIds;
        this.dialogNames = dialogNames;
    }
    
    /**
     * 将包数据编码到字节缓冲区
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(dialogIds.size());
        for (int i = 0; i < dialogIds.size(); i++) {
            buf.writeUtf(dialogIds.get(i));
            buf.writeUtf(dialogNames.get(i));
        }
    }
    
    /**
     * 从字节缓冲区解码包数据
     */
    public static ListDialogsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<String> ids = new ArrayList<>(size);
        List<String> names = new ArrayList<>(size);
        
        for (int i = 0; i < size; i++) {
            ids.add(buf.readUtf());
            names.add(buf.readUtf());
        }
        
        return new ListDialogsPacket(ids, names);
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
        // 在客户端显示对话列表
        Minecraft.getInstance().execute(() -> {
            for (int i = 0; i < dialogIds.size(); i++) {
                Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal(
                        "   - " + dialogIds.get(i) + " (" + dialogNames.get(i) + ")"
                    )
                );
            }
        });
    }
}