package top.yourzi.dialog.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.network.NetworkEvent;
import top.yourzi.dialog.Dialog;

import java.util.function.Supplier;

/**
 * 用于从客户端向服务器发送命令执行请求的网络包。
 */
public class ExecuteServerCommandPacket {
    private final String command;

    public ExecuteServerCommandPacket(String command) {
        this.command = command;
    }

    /**
     * 将包数据编码到字节缓冲区。
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.command);
    }

    /**
     * 从字节缓冲区解码包数据。
     */
    public static ExecuteServerCommandPacket decode(FriendlyByteBuf buf) {
        return new ExecuteServerCommandPacket(buf.readUtf());
    }

    /**
     * 处理接收到的包（在服务端）。
     */
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender(); // 获取发送此数据包的玩家
            if (sender == null) {
                Dialog.LOGGER.warn("ExecuteServerCommandPacket received from null sender.");
                return;
            }

            MinecraftServer server = sender.getServer();
            if (server == null) {
                Dialog.LOGGER.warn("ExecuteServerCommandPacket handler: MinecraftServer instance is null.");
                return;
            }


            Dialog.LOGGER.info("Server received request to execute command: {}", command);
            CommandSourceStack commandSource = server.createCommandSourceStack()
                                                 .withPermission(Commands.LEVEL_GAMEMASTERS) 
                                                 .withSuppressedOutput(); 

            // 设置命令源的位置和旋转角度为触发者的
            commandSource = commandSource.withPosition(sender.position()).withRotation(sender.getRotationVector());

            try {
                server.getCommands().performPrefixedCommand(commandSource, command);
                Dialog.LOGGER.info("Command executed successfully by server: {}", command);
            } catch (Exception e) {
                Dialog.LOGGER.error("Error executing command on server: {}", command, e);
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}