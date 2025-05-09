package top.yourzi.dialog.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.yourzi.dialog.Dialog;
import top.yourzi.dialog.DialogManager;
import top.yourzi.dialog.model.DialogSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 对话系统命令处理器。
 */
@Mod.EventBusSubscriber(modid = Dialog.MODID)
public class DialogCommand {

    /**
     * 注册命令。
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // 注册 /dialog 命令
        dispatcher.register(
            Commands.literal("dialog")
                .requires(source -> source.hasPermission(2)) // 要求权限等级2 (OP)
                .then(Commands.literal("show")
                    .then(Commands.argument("id", StringArgumentType.string())
                        .executes(context -> showDialog(context, StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("reload")
                    .executes(DialogCommand::reloadDialogs))
                .then(Commands.literal("list")
                    .executes(DialogCommand::listDialogs))
        );
    }
    
    /**
     * 显示指定ID的对话。
     */
    private static int showDialog(CommandContext<CommandSourceStack> context, String dialogId) {
        CommandSourceStack source = context.getSource();
        
        if (source.getEntity() instanceof ServerPlayer player) {
            // 向玩家发送网络包，在客户端显示对话
            source.sendSuccess(() -> Component.literal("正在显示对话: " + dialogId), false);

            // 使用 NetworkHandler 发送网络包到客户端
            top.yourzi.dialog.network.NetworkHandler.sendShowDialogToPlayer(player, dialogId);
            return 1;
        } else {
            source.sendFailure(Component.translatable("dialog.command.show.player_only"));
            return 0;
        }
    }
    
    /**
     * 重新加载所有对话。
     */
    private static int reloadDialogs(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // 服务器首先重新加载对话
        DialogManager.getInstance().loadDialogs(source.getServer().getResourceManager(), false);
        source.sendSuccess(() -> Component.translatable("dialog.command.reload.success"), false);

        // 通知客户端重新加载对话
        source.sendSuccess(() -> Component.translatable("dialog.command.reload.start"), false);

        // 如果是玩家执行的命令，则仅向该玩家发送网络包
        if (source.getEntity() instanceof ServerPlayer player) {
            top.yourzi.dialog.network.NetworkHandler.sendReloadDialogsToPlayer(player);
        } else {
            // 如果是服务器执行的命令，则向所有玩家发送网络包
            top.yourzi.dialog.network.NetworkHandler.sendReloadDialogsToAll();
        }
        
        return 1;
    }
    
    /**
     * 列出所有可用的对话。
     */
    private static int listDialogs(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // 列出所有对话
        source.sendSuccess(() -> Component.translatable("dialog.command.list.header"), false);

        // 获取所有对话的ID和名称
        List<String> dialogIds = new ArrayList<>();
        List<String> dialogNames = new ArrayList<>();

        // 从 DialogManager 获取所有对话序列
        Map<String, DialogSequence> dialogSequences = DialogManager.getInstance().getAllDialogSequences();
        dialogSequences.forEach((id, sequence) -> {
            dialogIds.add(id);
            dialogNames.add(sequence.getTitle());
        });

        // 如果是玩家执行的命令，则向该玩家发送网络包
        if (source.getEntity() instanceof ServerPlayer player) {
            top.yourzi.dialog.network.NetworkHandler.sendDialogListToPlayer(player, dialogIds, dialogNames);
        } else {
            // 如果是服务器执行的命令，则直接在服务器控制台显示
            source.sendSuccess(() -> Component.translatable("dialog.command.list.header"), false);
            for (int i = 0; i < dialogIds.size(); i++) {
                final int index = i;
                source.sendSuccess(() -> Component.literal("- " + dialogIds.get(index) + " (" + dialogNames.get(index) + ")"), false);
            }
        }
        
        return 1;
    }
}