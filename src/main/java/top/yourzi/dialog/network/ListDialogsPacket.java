package top.yourzi.dialog.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.yourzi.dialog.Dialog;

import java.util.ArrayList;
import java.util.List;

public record ListDialogsPacket(List<String> dialogIds, List<String> dialogNames) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ListDialogsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dialog.MODID, "list_dialogs_packet"));

    public static final StreamCodec<FriendlyByteBuf, ListDialogsPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
        ListDialogsPacket::dialogIds,
        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
        ListDialogsPacket::dialogNames,
        ListDialogsPacket::new
    );

    @Override
    public Type<ListDialogsPacket> type() {
        return TYPE;
    }

    public List<String> getDialogIds() {
        return dialogIds;
    }

    public List<String> getDialogNames() {
        return dialogNames;
    }

    public static void handleClient(final ListDialogsPacket message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            executeOnClient(message);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void executeOnClient(final ListDialogsPacket message) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Available Dialogs:"));
                for (int i = 0; i < message.getDialogIds().size(); i++) {
                    Minecraft.getInstance().player.sendSystemMessage(
                        Component.literal("   - " + message.getDialogIds().get(i) + " (" + message.getDialogNames().get(i) + ")")
                    );
                }
            }
        });
    }
}