package top.yourzi.dialog.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.yourzi.dialog.Dialog;
import top.yourzi.dialog.DialogManager;
import top.yourzi.dialog.model.DialogSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 对话系统命令处理器。
 */
@EventBusSubscriber(modid = Dialog.MODID)
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
            DialogManager dialogManager = DialogManager.getInstance();
            DialogSequence originalSequence = dialogManager.getDialogSequence(dialogId);

            if (originalSequence == null) {
                source.sendFailure(Component.literal("Dialog with ID '" + dialogId + "' not found."));
                return 0;
            }

            // 为玩家创建特定对话序列 (过滤选项)
            DialogSequence playerSpecificSequence = dialogManager.createPlayerSpecificSequence(originalSequence, player, source.getServer());
            if (playerSpecificSequence == null) {
                 source.sendFailure(Component.literal("Failed to create player-specific dialog for ID '" + dialogId + "'."));
                 return 0;
            }

            // 将过滤后的对话序列转换为JSON
            String dialogJson = DialogManager.GSON.toJson(playerSpecificSequence);

            // 发送包含完整对话数据的包
            top.yourzi.dialog.network.NetworkHandler.sendShowDialogToPlayer(player, dialogId, dialogJson);
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
        
        //服务器重新加载对话
        DialogManager.getInstance().loadDialogsFromServer(source.getServer().getResourceManager());
        source.sendSuccess(() -> Component.translatable("dialog.command.reload.success_server"), true); // Notify command executor

        //获取所有对话的JSON数据
        Map<String, String> allDialogJsons = DialogManager.getInstance().getAllDialogJsonsForSync();

        //向所有玩家同步新的对话数据
        if (!allDialogJsons.isEmpty()) {
            top.yourzi.dialog.network.NetworkHandler.sendAllDialogsToAllPlayers(allDialogJsons);
            source.sendSuccess(() -> Component.translatable("dialog.command.reload.sync_sent_all", allDialogJsons.size()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("dialog.command.reload.no_dialogs_to_sync"), true);
            top.yourzi.dialog.network.NetworkHandler.sendAllDialogsToAllPlayers(new java.util.HashMap<>());
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