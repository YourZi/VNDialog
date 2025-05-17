package top.yourzi.dialog.network;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.yourzi.dialog.Dialog;

/**
 * 用于从客户端向服务器发送命令执行请求的网络包 (C2S).
 */
public record ExecuteServerCommandPacket(String command) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<ExecuteServerCommandPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dialog.MODID, "execute_server_command_packet"));

   public static final StreamCodec<FriendlyByteBuf, ExecuteServerCommandPacket> STREAM_CODEC = StreamCodec.composite(
    ByteBufCodecs.STRING_UTF8,
    ExecuteServerCommandPacket::command,
    ExecuteServerCommandPacket::new
);

    public String getCommand() {
        return command;
    }

    /**
     * 处理接收到的包（在服务端）。
     * 方法名和签名需要匹配 NetworkHandler 中注册的处理器。
     */
    public static void handleServer(final ExecuteServerCommandPacket message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer sender = (ServerPlayer) context.player();
            if (sender == null) {
                Dialog.LOGGER.warn("ExecuteServerCommandPacket received from null sender.");
                return;
            }

            MinecraftServer server = sender.getServer();
            if (server == null) {
                Dialog.LOGGER.warn("ExecuteServerCommandPacket handler: MinecraftServer instance is null.");
                return;
            }

            CommandSourceStack commandSource = sender.createCommandSourceStack()
                                                 .withPermission(Commands.LEVEL_GAMEMASTERS)
                                                 .withSuppressedOutput();

            try {
                server.getCommands().performPrefixedCommand(commandSource, message.getCommand());
            } catch (Exception e) {
                Dialog.LOGGER.error("Error executing command on server: {}", message.getCommand(), e);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}