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

            // 使用服务器的命令源执行命令，这将拥有完全权限
            // CommandSourceStack source = server.createCommandSourceStack().withPermission(4).withSuppressedOutput(); // 最高权限且不向玩家发送命令输出
            // 为了确保命令在正确的上下文中执行（例如，如果命令中包含 @p），我们可能需要基于玩家创建命令源，但赋予其更高权限
            // 或者，如果命令总是设计为由服务器本身执行，则可以直接使用服务器的命令源。
            // 对于大多数情况，直接使用服务器的命令源并指定执行上下文（如服务器级别和位置）是安全的。

            Dialog.LOGGER.info("Server received request to execute command: {}", command);
            CommandSourceStack commandSource = server.createCommandSourceStack()
                                                 .withPermission(Commands.LEVEL_GAMEMASTERS) // 通常是OP权限级别
                                                 .withSuppressedOutput(); // 禁止命令输出到聊天，除非命令本身产生
            
            // 如果需要，可以进一步定制命令源，例如设置执行位置等
            // commandSource = commandSource.withPosition(sender.position()).withRotation(sender.getRotationVector());

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