package top.yourzi.dialog.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.yourzi.dialog.Dialog;
import top.yourzi.dialog.DialogManager;

/**
 * 重新加载对话的网络包 (C2S -> S2C)
 * 客户端请求 -> 服务端处理 -> 服务端发送给所有客户端
 * 或者由服务端直接触发发送给所有客户端
 */
public record ReloadDialogsPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ReloadDialogsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dialog.MODID, "reload_dialogs_packet"));

    public static final StreamCodec<ByteBuf, ReloadDialogsPacket> STREAM_CODEC = StreamCodec.composite(
        null, null, null
    );

    public ReloadDialogsPacket() {
        // 空构造函数，用于从字节流解码时实例化
    }

    /**
     * 从字节缓冲区解码包数据。
     * NeoForge 要求一个接收 FriendlyByteBuf 的构造函数用于解码。
     */
    public ReloadDialogsPacket(FriendlyByteBuf buf) {
        this();
    }

    /**
     * 将包数据编码到字节缓冲区。
     * NeoForge 要求 write 方法用于编码。
     */
    public void write(FriendlyByteBuf buf) {
        // 此数据包没有数据需要写入
    }

    /**
     * 处理接收到的包 (在客户端执行)。
     * 方法名和签名需要匹配 NetworkHandler 中注册的处理器。
     */
    public static void handleClient(final ReloadDialogsPacket message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // 确保在客户端线程中执行
            executeOnClient(message);
        });
    }

    /**
     * 在客户端处理包的具体逻辑。
     */
    @OnlyIn(Dist.CLIENT)
    private static void executeOnClient(final ReloadDialogsPacket message) {
        Minecraft.getInstance().execute(() -> {
            DialogManager.getInstance().loadDialogsFromServer(Minecraft.getInstance().getResourceManager());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}