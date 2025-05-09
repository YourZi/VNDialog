package top.yourzi.dialog.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import top.yourzi.dialog.DialogManager;
import top.yourzi.dialog.Dialog;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 网络数据包，用于从服务端向客户端同步所有对话数据。
 */
public class SyncAllDialogsPacket {
    private final Map<String, String> dialogDataMap;

    public SyncAllDialogsPacket(Map<String, String> dialogDataMap) {
        this.dialogDataMap = dialogDataMap;
    }

    /**
     * 将包数据编码到字节缓冲区。
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(dialogDataMap.size());
        dialogDataMap.forEach((id, json) -> {
            buf.writeUtf(id);
            buf.writeUtf(json);
        });
        Dialog.LOGGER.debug("编码 SyncAllDialogsPacket，包含 {} 个对话数据", dialogDataMap.size());
    }

    /**
     * 从字节缓冲区解码包数据。
     */
    public static SyncAllDialogsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, String> dialogDataMap = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            String json = buf.readUtf();
            dialogDataMap.put(id, json);
        }
        Dialog.LOGGER.debug("解码 SyncAllDialogsPacket，包含 {} 个对话数据", dialogDataMap.size());
        return new SyncAllDialogsPacket(dialogDataMap);
    }

    /**
     * 处理接收到的包 (在客户端执行)。
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
     * 在客户端处理包的具体逻辑。
     */
    @OnlyIn(Dist.CLIENT)
    private void handleOnClient() {
        Dialog.LOGGER.info("客户端收到 SyncAllDialogsPacket，准备同步 {} 个对话数据。", dialogDataMap.size());
        DialogManager.getInstance().receiveAllDialogsFromServer(this.dialogDataMap);
    }
}