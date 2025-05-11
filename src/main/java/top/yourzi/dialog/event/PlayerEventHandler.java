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
            Map<String, String> allDialogJsons = DialogManager.getInstance().getAllDialogJsonsForSync();
            if (!allDialogJsons.isEmpty()) {
                NetworkHandler.sendAllDialogsToPlayer(player, allDialogJsons);
            } else {
                // 如果没有对话数据，也发送一个空包，以确保客户端清空旧缓存（如果存在）
                NetworkHandler.sendAllDialogsToPlayer(player, new java.util.HashMap<>());
                Dialog.LOGGER.warn("There is no conversation data to be synchronized to the player {}, send an empty sync packet to clear the client cache.", player.getName().getString());
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
                    DialogManager.getInstance().loadDialogsFromServer(resourceManager);
                }, backgroundExecutor).thenCompose(preparationBarrier::wait);
            }

            @Override
            public String getName() {
                return Dialog.MODID + "_dialog_reloader"; // 给监听器一个唯一的名字
            }
        });
    }
}