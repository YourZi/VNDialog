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

import java.util.HashMap;
import java.util.Map;

public record SyncAllDialogsPacket(Map<String, String> dialogDataMap) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncAllDialogsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dialog.MODID, "sync_all_dialogs_packet"));

    public static final StreamCodec<FriendlyByteBuf, SyncAllDialogsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8),
            SyncAllDialogsPacket::dialogDataMap,
            SyncAllDialogsPacket::new
    );

    public Map<String, String> getDialogDataMap() {
        return dialogDataMap;
    }

    public static void handleClient(final SyncAllDialogsPacket message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            executeOnClient(message);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void executeOnClient(final SyncAllDialogsPacket message) {
        DialogManager.getInstance().receiveAllDialogsFromServer(message.getDialogDataMap());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}