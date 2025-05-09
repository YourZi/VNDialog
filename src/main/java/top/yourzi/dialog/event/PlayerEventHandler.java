package top.yourzi.dialog.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.yourzi.dialog.Dialog;
import top.yourzi.dialog.DialogManager;
import top.yourzi.dialog.network.NetworkHandler;
import java.util.Map;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mod.EventBusSubscriber(modid = Dialog.MODID)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Dialog.LOGGER.info("玩家 {} 已登录，准备同步对话数据...", player.getName().getString());
            Map<String, String> allDialogJsons = DialogManager.getInstance().getAllDialogJsonsForSync();
            if (!allDialogJsons.isEmpty()) {
                NetworkHandler.sendAllDialogsToPlayer(player, allDialogJsons);
                Dialog.LOGGER.info("已向玩家 {} 发送 {} 个对话数据进行同步。", player.getName().getString(), allDialogJsons.size());
            } else {
                // 如果没有对话数据，也发送一个空包，以确保客户端清空旧缓存（如果存在）
                NetworkHandler.sendAllDialogsToPlayer(player, new java.util.HashMap<>());
                Dialog.LOGGER.info("没有对话数据需要同步给玩家 {}，发送空同步包以清空客户端缓存。", player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager,
                                                  ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, 
                                                  Executor backgroundExecutor, Executor gameExecutor) {
                return CompletableFuture.runAsync(() -> {
                    Dialog.LOGGER.info("数据包重载事件触发，开始加载对话数据...");
                    DialogManager.getInstance().loadDialogsFromServer(resourceManager);
                    // 数据加载后，需要通知所有在线玩家同步新的对话数据
                    // 这通常在 loadDialogsFromServer 内部不直接处理，
                    // 而是由 /reload 命令（DialogCommand）显式触发同步，
                    // 或者在玩家加入时（onPlayerLoggedIn）同步。
                    // 如果希望每次数据包重载都自动同步给所有玩家，可以在这里添加：
                    // Map<String, String> allDialogJsons = DialogManager.getInstance().getAllDialogJsonsForSync();
                    // if (event.getServer() != null) { // 确保服务器实例存在
                    //     NetworkHandler.sendAllDialogsToAllPlayers(allDialogJsons);
                    //     Dialog.LOGGER.info("数据包重载后，已向所有客户端发送 {} 个对话数据进行同步。", allDialogJsons.size());
                    // } else {
                    //     Dialog.LOGGER.warn("数据包重载后，无法获取服务器实例来同步对话数据。");
                    // }
                    // 当前设计下，/dialog reload 会处理同步，玩家登录也会同步，这里仅加载数据到服务端即可。
                }, backgroundExecutor).thenCompose(preparationBarrier::wait);
            }

            @Override
            public String getName() {
                return Dialog.MODID + "_dialog_reloader"; // 给监听器一个唯一的名字
            }
        });
        Dialog.LOGGER.info("已注册对话数据重载监听器。");
    }
}