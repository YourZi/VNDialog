package top.yourzi.dialog.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.yourzi.dialog.Dialog;
import top.yourzi.dialog.DialogManager;

public record ShowDialogPacket(String dialogId, String dialogJson) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShowDialogPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dialog.MODID, "show_dialog_packet"));

    public static final StreamCodec<ByteBuf, ShowDialogPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ShowDialogPacket::dialogId,
            ByteBufCodecs.STRING_UTF8,
            ShowDialogPacket::dialogJson,
            ShowDialogPacket::new
    );



    public static void handleClient(final ShowDialogPacket message, final IPayloadContext context) {
        // 在客户端显示对话
        Minecraft.getInstance().execute(() -> {
            if (message.dialogJson != null && !message.dialogJson.isEmpty()) {
                DialogManager.getInstance().receiveAndShowPlayerSpecificDialog(message.dialogId, message.dialogJson);
            } else {
                top.yourzi.dialog.Dialog.LOGGER.warn("ShowDialogPacket received for id '{}' but dialogJson is empty. Client will not show dialog via this packet.", message.dialogId);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void executeOnClient(final ShowDialogPacket message) {
        Minecraft.getInstance().execute(() -> {
            DialogManager.getInstance().showDialog(message.dialogId());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}