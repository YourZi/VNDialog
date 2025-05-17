package top.yourzi.dialog.network;

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

public record SendDialogDataPacket(String dialogId, String dialogJson) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SendDialogDataPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dialog.MODID, "send_dialog_data_packet"));

    public static final StreamCodec<FriendlyByteBuf, SendDialogDataPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        SendDialogDataPacket::dialogId,
        ByteBufCodecs.STRING_UTF8,
        SendDialogDataPacket::dialogJson,
        SendDialogDataPacket::new
    );

    public String getDialogId() {
        return dialogId;
    }

    public String getDialogJson() {
        return dialogJson;
    }

    public static void handleClient(final SendDialogDataPacket message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            executeOnClient(message);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void executeOnClient(final SendDialogDataPacket message) {
        DialogManager.getInstance().receiveDialogData(message.getDialogId(), message.getDialogJson());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}