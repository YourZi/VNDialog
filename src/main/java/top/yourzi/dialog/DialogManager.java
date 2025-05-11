package top.yourzi.dialog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import top.yourzi.dialog.model.DialogEntry;
import top.yourzi.dialog.model.DialogSequence;
import top.yourzi.dialog.model.SubmitItemInfo;
import top.yourzi.dialog.ui.DialogScreen;
import top.yourzi.dialog.network.NetworkHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

import net.minecraft.world.entity.player.Player;
/**
 * 对话系统的核心管理类，负责加载和管理对话序列。
 */
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public class DialogManager {
    private static final Gson GSON = new GsonBuilder().create();
    private static final DialogManager INSTANCE = new DialogManager();

    // 服务端: 存储所有从数据包加载的对话序列
    // 客户端: 存储从服务端同步过来的对话序列
    private final Map<String, DialogSequence> dialogSequences = new HashMap<>();
    // 当前显示的对话序列
    private DialogSequence currentSequence;
    // 当前显示的对话条目
    private DialogEntry currentEntry;
    // 对话历史记录
    private final List<DialogEntry> dialogHistory = new ArrayList<>();
    // 标记下一次对话推进是否由快速跳过触发
    private static boolean isFastForwardingNext = false;
    // 自动播放状态
    private static boolean isAutoPlaying = false;

    private DialogManager() {}
    
    /**
     * 向玩家发送消息。
     */
    @OnlyIn(Dist.CLIENT)
    private void sendPlayerMessage(Component message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(message);
        }
    }
    
    public static DialogManager getInstance() {
        return INSTANCE;
    }

    /**
     * 加载所有对话序列 (仅服务端调用)。
     * 此方法从数据包 (data/<modid>/dialogs/) 加载对话。
     * @param resourceManager 资源管理器实例。
     */
    public void loadDialogsFromServer(ResourceManager resourceManager) {
        dialogSequences.clear();
        Dialog.LOGGER.info("The server is loading the dialog file from the datapack......");

        Map<ResourceLocation, Resource> modSpecificResources = resourceManager.listResources("dialogs", resource -> resource.getPath().endsWith(".json")).entrySet().stream()
            .filter(entry -> entry.getKey().getNamespace().equals(Dialog.MODID))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Dialog.LOGGER.info("Find {} JSON file in the directory", modSpecificResources.size());

        modSpecificResources.forEach((resourceLocation, resource) -> {
            Dialog.LOGGER.info("Dialog files being processed. {}", resourceLocation);
            try {
                DialogSequence sequence = parseDialogSequenceFromFile(resource); // 解析对话序列
                if (sequence != null && sequence.getId() != null) {
                    dialogSequences.put(sequence.getId(), sequence);
                    Dialog.LOGGER.info("Successfully loaded dialog sequence. {}", sequence.getId());
                } else {
                    Dialog.LOGGER.warn("Empty dialog sequence or empty ID. {}", resourceLocation);
                }
            } catch (Exception e) {
                Dialog.LOGGER.error("Failed to load dialog file {}: {}", resourceLocation, e.getMessage(), e);
            }
        });

        Dialog.LOGGER.info("The server loaded a total of {} dialog sequences", dialogSequences.size());
        if (dialogSequences.isEmpty()) {
            Dialog.LOGGER.warn("No dialog sequence was found, please check the 'dialogs' directory ('data/{}/dialogs') or file format in the datapack.", Dialog.MODID);
        }
    }

    /**
     * 解析对话序列JSON文件 (内部使用, 服务端加载时调用)。
     * @param resource 资源文件。
     */
    private DialogSequence parseDialogSequenceFromFile(Resource resource) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            return GSON.fromJson(reader, DialogSequence.class);
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            Dialog.LOGGER.error("Failure to read or parse dialog JSON file ({}): {}", resource.sourcePackId(), e.getMessage());
            // 尝试读取内容以进行更详细的调试
            try (BufferedReader contentReader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                StringBuilder jsonContent = new StringBuilder();
                String line;
                while ((line = contentReader.readLine()) != null) {
                    jsonContent.append(line);
                }
                Dialog.LOGGER.debug("JSON: {}", jsonContent.toString());
            } catch (IOException ioe) {
                Dialog.LOGGER.error("Unable to read problematic JSON content for debugging. {}", ioe.getMessage());
            }
            return null;
        }
    }

    /**
     * (客户端) 清空所有已缓存的对话数据。
     * 通常在从服务器断开或服务器重载数据包时调用。
     */
    @OnlyIn(Dist.CLIENT)
    public void clearAllDialogsOnClient() {
        if (Minecraft.getInstance() == null || !Minecraft.getInstance().level.isClientSide) return;
        dialogSequences.clear();
        currentSequence = null;
        currentEntry = null;
        clearDialogHistory();
        Dialog.LOGGER.info("The client dialog cache has been cleared.");
    }

    /**
     * (客户端) 接收并缓存从服务器同步过来的所有对话数据。
     * @param dialogDataMap 一个映射，键是对话ID，值是对话内容的JSON字符串。
     */
    @OnlyIn(Dist.CLIENT)
    public void receiveAllDialogsFromServer(Map<String, String> dialogDataMap) {
        if (Minecraft.getInstance() == null || !Minecraft.getInstance().level.isClientSide) return;
        clearAllDialogsOnClient(); // 先清空旧数据
        Dialog.LOGGER.info("The client receives {} conversation data for synchronization.", dialogDataMap.size());
        dialogDataMap.forEach((id, json) -> {
            try {
                DialogSequence sequence = GSON.fromJson(json, DialogSequence.class);
                if (sequence != null && sequence.getId() != null) {
                    if (!id.equals(sequence.getId())) {
                        Dialog.LOGGER.warn("Dialog ID mismatch! Expected ID: {}, ID in JSON: {}. Will use expected ID.", id, sequence.getId());
                    }
                    dialogSequences.put(id, sequence); // 使用map的key作为权威ID
                    Dialog.LOGGER.debug("Client Successfully Cached Conversation. {}", id);
                } else {
                    Dialog.LOGGER.warn("Parsing of the dialog data received from the server failed or the ID is null. ID: {}, JSON: {}", id, json);
                }
            } catch (com.google.gson.JsonSyntaxException e) {
                Dialog.LOGGER.error("Failed to parse the dialog JSON received from the server. ID: {}, 错误: {}", id, e.getMessage());
                Dialog.LOGGER.debug("(ID: {}): {}", id, json, e);
            }
        });
        Dialog.LOGGER.info("Client conversation data synchronization is complete, currently caching {} conversations.", dialogSequences.size());
        if (dialogSequences.isEmpty() && !dialogDataMap.isEmpty()) {
            Dialog.LOGGER.warn("Dialog data has been received but the cache is empty after parsing, please check the JSON format and content.");
        }
    }

    /**
     * 将对话条目添加到历史记录。
     */
    @OnlyIn(Dist.CLIENT)
    private void addDialogToHistory(DialogEntry entry) {
        if (entry != null) {
            dialogHistory.add(entry);
        }
    }

    /**
     * 获取对话历史记录。
     */
    @OnlyIn(Dist.CLIENT)
    public List<DialogEntry> getDialogHistory() {
        if (Minecraft.getInstance() == null || !Minecraft.getInstance().level.isClientSide) return Collections.emptyList();
        return new ArrayList<>(dialogHistory);
    }

    /**
     * 清空对话历史记录。
     */
    @OnlyIn(Dist.CLIENT)
    private void clearDialogHistory() {
        dialogHistory.clear();
    }

    /**
     * 记录玩家在当前对话中选择的选项。
     * @param optionText 所选选项的文本。
     */
    @OnlyIn(Dist.CLIENT)
    public void recordChoiceForCurrentDialog(String optionText) {
        if (Minecraft.getInstance() == null || !Minecraft.getInstance().level.isClientSide) return;
        if (currentEntry != null) {
            currentEntry.setSelectedOptionText(optionText);
            // 更新历史记录中最新的对应条目
            if (!dialogHistory.isEmpty()) {
                DialogEntry lastHistoryEntry = dialogHistory.get(dialogHistory.size() - 1);
                // 确保更新的是同一个对话条目（理论上应该是同一个）
                if (lastHistoryEntry == currentEntry) {
                    lastHistoryEntry.setSelectedOptionText(optionText);
                } else {
                    // 如果不是同一个条目，可能存在逻辑错误，或者 currentEntry 在添加到历史记录后被更改。
                    // 尝试通过ID查找并更新。
                    for (int i = dialogHistory.size() - 1; i >= 0; i--) {
                        if (dialogHistory.get(i).getId() != null && dialogHistory.get(i).getId().equals(currentEntry.getId())) {
                            dialogHistory.get(i).setSelectedOptionText(optionText);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * (服务端) 获取所有对话序列的JSON表示，用于发送给客户端。
     */
    public Map<String, String> getAllDialogJsonsForSync() {
        Map<String, String> dialogJsons = new HashMap<>();
        dialogSequences.forEach((id, sequence) -> {
            dialogJsons.put(id, GSON.toJson(sequence));
        });
        return dialogJsons;
    }

    /**
     * 根据ID获取对话序列。
     * 服务端：从加载的对话中获取。
     * 客户端：从缓存的对话中获取。
     */
    public DialogSequence getDialogSequence(String id) {
        return dialogSequences.get(id);
    }
    
    /**
     * 获取所有对话序列。
     */
    public Map<String, DialogSequence> getAllDialogSequences() {
        return new HashMap<>(dialogSequences);
    }
    
    /**
     * 显示指定ID的对话序列。
     */
    @OnlyIn(Dist.CLIENT)
    public void receiveDialogData(String dialogId, String dialogJson) {
        if (Minecraft.getInstance() == null || !Minecraft.getInstance().level.isClientSide) return;
        Dialog.LOGGER.info("Client receives dialog data: {}", dialogId);
        try {
            DialogSequence sequence = GSON.fromJson(dialogJson, DialogSequence.class);
            if (sequence != null && sequence.getId() != null) {
                dialogSequences.put(sequence.getId(), sequence);
                Dialog.LOGGER.info("Successfully parsing and storing conversations received from the server side: {}", sequence.getId());
                // 确保在主线程显示对话界面
                Minecraft.getInstance().execute(() -> showDialog(dialogId)); // Now show the dialog
            } else {
                Dialog.LOGGER.warn("Failed to parse the dialog data received from the server or the ID is null: {}", dialogId);
                sendPlayerMessage(Component.translatable("dialog.manager.received_sequence_empty", dialogId));
            }
        } catch (Exception e) {
            Dialog.LOGGER.error("Failed to parse dialog '{}' JSON received from server", dialogId);
            sendPlayerMessage(Component.translatable("dialog.manager.received_parse_failed", dialogId, e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * 显示指定ID的对话序列。
     */
    @OnlyIn(Dist.CLIENT)
    public void showDialog(String dialogId) {
        if (Minecraft.getInstance() == null || !Minecraft.getInstance().level.isClientSide) return;
        stopAutoPlay(); // 每次对话启动时重置自动播放为关闭状态
        DialogSequence sequence = getDialogSequence(dialogId);
        if (sequence == null) {
            Dialog.LOGGER.info("Dialog '{}' was not found locally and is being requested from the server...", dialogId);
            NetworkHandler.sendRequestDialogToServer(dialogId);
            sendPlayerMessage(Component.translatable("dialog.manager.requesting_from_server", dialogId));
            return;
        }
        
        clearDialogHistory(); // 开始新对话时清空历史记录
        currentSequence = sequence;
        currentEntry = sequence.getFirstEntry();
        addDialogToHistory(currentEntry); // 将第一个条目加入历史记录
        
        if (currentEntry == null) {
            Dialog.LOGGER.error("No entries found in dialog sequence: {}", dialogId);
            sendPlayerMessage(Component.translatable("dialog.manager.no_entries", dialogId));
            return;
        }
        
        // 显示对话界面
        Minecraft.getInstance().setScreen(new DialogScreen(currentSequence, currentEntry));
    }

    /**
     * 获取快速跳过标记。
     */
    public static boolean isFastForwardingNext() {
        return isFastForwardingNext;
    }

    /**
     * 设置快速跳过标记。
     */
    public static void setFastForwardingNext(boolean fastForwardingNext) {
        isFastForwardingNext = fastForwardingNext;
    }

    /**
     * 获取自动播放状态。
     */
    public static boolean isAutoPlaying() {
        return isAutoPlaying;
    }

    /**
     * 设置自动播放状态。
     */
    public static void setAutoPlaying(boolean autoPlaying) {
        isAutoPlaying = autoPlaying;
    }

    /**
     * 停止自动播放。
     */
    public static void stopAutoPlay() {
        isAutoPlaying = false;
    }
    
    /**
     * 显示对话序列中的下一条对话。
     */
    @OnlyIn(Dist.CLIENT)
    public void showNextDialog() {
        if (Minecraft.getInstance() == null || !Minecraft.getInstance().level.isClientSide) return;
        if (currentSequence == null || currentEntry == null) {
            return;
        }
        
        DialogEntry nextEntry = currentSequence.getNextEntry(currentEntry);
        if (nextEntry == null) {
            // 对话结束，关闭对话界面
            Minecraft.getInstance().setScreen(null);
            currentSequence = null;
            currentEntry = null;
            return;
        }
        
        currentEntry = nextEntry;
        addDialogToHistory(currentEntry); // 将后续条目加入历史记录
        // 更新对话界面
        Minecraft.getInstance().setScreen(new DialogScreen(currentSequence, currentEntry));
    }
    /**
     * 根据选项跳转到指定的对话。
     */
    @OnlyIn(Dist.CLIENT)
    public void jumpToDialog(String targetId) {
        if (Minecraft.getInstance() == null || !Minecraft.getInstance().level.isClientSide) return;
        if (currentSequence == null) {
            return;
        }
        
        DialogEntry targetEntry = currentSequence.findEntryById(targetId);
        if (targetEntry == null) {
            Dialog.LOGGER.error("Target dialog entry not found: {}", targetId);
            sendPlayerMessage(Component.translatable("dialog.manager.target_not_found", targetId));
            return;
        }
        
        currentEntry = targetEntry;
        addDialogToHistory(currentEntry); // 将跳转的条目加入历史记录
        Minecraft.getInstance().setScreen(new DialogScreen(currentSequence, currentEntry));
    }

    /**
     * (服务端) 处理客户端提交物品的请求。
     */
    public void handleSubmitItem(ServerPlayer player, String clientDialogId, String clientItemId, String clientItemNbt, int clientItemCount) {
        Dialog.LOGGER.info("Player {} attempting to submit item: id={}, nbt={}, count={} for dialogId: {}", 
            player.getName().getString(), clientItemId, clientItemNbt, clientItemCount, clientDialogId);

        DialogSequence sequence = getDialogSequenceForPlayer(player, clientDialogId); // 获取当前对话序列
        if (sequence == null) {
            Dialog.LOGGER.warn("DialogSequence not found for player {} and dialogId {}. Cannot process item submission.", player.getName().getString(), clientDialogId);
            // 可以选择向玩家发送错误消息
            return;
        }

        DialogEntry entry = sequence.findEntryById(clientDialogId); // 查找当前对话条目
        if (entry == null || entry.getSubmitItemInfo() == null) {
            Dialog.LOGGER.warn("DialogEntry or SubmitItemInfo not found for dialogId {}. Cannot process item submission.", clientDialogId);
            return;
        }

        SubmitItemInfo submitInfo = entry.getSubmitItemInfo();

        // 校验客户端发送的数据是否与服务器端配置一致 (防止恶意修改)
        if (!submitInfo.getItemId().equals(clientItemId) || 
            (submitInfo.getItemNbt() != null && !submitInfo.getItemNbt().equals(clientItemNbt)) || 
            (submitInfo.getItemNbt() == null && clientItemNbt != null) || 
            submitInfo.getItemCount() != clientItemCount) {
            Dialog.LOGGER.warn("Client item submission data mismatch for player {}. Expected: id={}, nbt={}, count={}. Actual: id={}, nbt={}, count={}",
                player.getName().getString(), submitInfo.getItemId(), submitInfo.getItemNbt(), submitInfo.getItemCount(),
                clientItemId, clientItemNbt, clientItemCount);
            return;
        }

        // 检查玩家背包中是否有足够的物品
        boolean hasEnoughItems = checkAndRemovePlayerItems(player, submitInfo.getItemId(), submitInfo.getItemNbt(), submitInfo.getItemCount());

        if (hasEnoughItems) {
            Dialog.LOGGER.info("Player {} successfully submitted items. Proceeding to target dialog and command execution.", player.getName().getString());
            // 执行指令 (如果存在)
            if (submitInfo.getCommand() != null && !submitInfo.getCommand().isEmpty()) {
                executeCommand(player, submitInfo.getCommand());
            }
            // 跳转到目标对话
            if (submitInfo.getTargetDialogId() != null && !submitInfo.getTargetDialogId().isEmpty()) {
                NetworkHandler.sendShowDialogToPlayer(player, submitInfo.getTargetDialogId());
            } else {
                Dialog.LOGGER.warn("TargetDialogId is null or empty for item submission in dialog {}. Player will remain in current dialog.", clientDialogId);
            }
        } else {
            Dialog.LOGGER.info("Player {} does not have enough items to submit.", player.getName().getString());
        }
    }

    /**
     * (服务端) 获取玩家当前对话序列。由于客户端可能不同步，这里需要一种方式获取。
     * 简单实现：假设玩家当前对话就是请求的dialogId对应的序列。
     * 复杂实现：可能需要追踪每个玩家的当前对话状态。
     */
    private DialogSequence getDialogSequenceForPlayer(ServerPlayer player, String dialogId) {
        DialogSequence sequence = dialogSequences.get(dialogId);
        if (sequence != null) return sequence;

        // 尝试查找包含此dialogId的序列
        for (DialogSequence seq : dialogSequences.values()) {
            if (seq.findEntryById(dialogId) != null) {
                return seq;
            }
        }
        return null;
    }

    /**
     * (服务端) 在服务器上代表玩家执行命令。
     */
    public void executeCommand(Player player, String command) {
        if (player.getServer() != null) {
            player.getServer().getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(2),
                command
            );
        }
    }

    /**
     * (服务端) 检查并移除玩家背包中的物品。
     * @return 如果成功移除返回true，否则返回false。
     */
    private boolean checkAndRemovePlayerItems(ServerPlayer player, String itemId, String itemNbtStr, int requiredCount) {
        net.minecraft.world.item.Item targetItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (targetItem == null || targetItem == Items.AIR) {
            Dialog.LOGGER.warn("Invalid item ID for submission: {}", itemId);
            return false;
        }

        CompoundTag requiredNbt = null;
        if (itemNbtStr != null && !itemNbtStr.isEmpty()) {
            try {
                requiredNbt = TagParser.parseTag(itemNbtStr);
            } catch (Exception e) {
                Dialog.LOGGER.warn("Invalid NBT string for item submission: {}. Error: {}", itemNbtStr, e.getMessage());
                return false;
            }
        }

        int foundCount = 0;
        List<Integer> slotsToRemove = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack stackInSlot = player.getInventory().getItem(i);
            if (!stackInSlot.isEmpty() && stackInSlot.getItem() == targetItem) {
                boolean nbtMatch = (requiredNbt == null) || (stackInSlot.hasTag() && requiredNbt.equals(stackInSlot.getTag()));
                if (nbtMatch) {
                    foundCount += stackInSlot.getCount();
                }
            }
        }

        if (foundCount < requiredCount) {
            return false; // 物品不足
        }

        // 移除物品
        int countToRemove = requiredCount;
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            if (countToRemove <= 0) break;
            ItemStack stackInSlot = player.getInventory().getItem(i);
            if (!stackInSlot.isEmpty() && stackInSlot.getItem() == targetItem) {
                boolean nbtMatch = (requiredNbt == null) || (stackInSlot.hasTag() && requiredNbt.equals(stackInSlot.getTag()));
                if (nbtMatch) {
                    int amountInStack = stackInSlot.getCount();
                    int amountToTake = Math.min(countToRemove, amountInStack);
                    stackInSlot.shrink(amountToTake);
                    countToRemove -= amountToTake;
                    if (stackInSlot.isEmpty()) {
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        player.getInventory().setChanged(); // 通知背包更新
        return true;
    }
}
