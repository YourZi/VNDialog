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
import top.yourzi.dialog.model.DialogEntry;
import top.yourzi.dialog.model.DialogSequence;
import top.yourzi.dialog.ui.DialogScreen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 对话系统的核心管理类，负责加载和管理对话序列。
 */
public class DialogManager {
    private static final Gson GSON = new GsonBuilder().create();
    private static final DialogManager INSTANCE = new DialogManager();
    
    // 存储所有已加载的对话序列
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
    
    private DialogManager() {
    }
    
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
     * 执行命令字符串。
     * 当对话条目或选项指定命令时，调用此方法。
     * @param command 要执行的命令字符串。
     */
    @OnlyIn(Dist.CLIENT)
    public void executeCommand(String command) {
        if (command != null && !command.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                Dialog.LOGGER.info("Executing command from dialog system: {}", command);
                // 此操作将像玩家在聊天中输入命令一样发送命令：
                // - 服务器命令由服务器处理。
                // - 客户端命令（例如 /clear）由客户端处理。
                mc.player.connection.sendCommand(command);
            } else {
                Dialog.LOGGER.warn("Player is null, cannot execute command: {}", command);
            }
        }
    }
    
    /**
     * 加载所有对话序列。
     * @param resourceManager 资源管理器实例。
     * @param isClientContext 是否为客户端上下文。
     */
    public void loadDialogs(ResourceManager resourceManager, boolean isClientContext) {
        dialogSequences.clear();
        
        Dialog.LOGGER.info("准备从所有命名空间的 'dialogs' 目录加载对话文件, 目标模组: {}", Dialog.MODID);
        if (isClientContext) {
            sendPlayerMessage(Component.translatable("dialog.manager.loading", "dialogs"));
        }
        
        // 获取所有命名空间下 "dialogs" 目录中的JSON文件
        Map<ResourceLocation, Resource> allFoundResources = resourceManager.listResources("dialogs", resource -> resource.getPath().endsWith(".json"));

        // 筛选出属于当前模组的资源
        Map<ResourceLocation, Resource> modSpecificResources = allFoundResources.entrySet().stream()
            .filter(entry -> entry.getKey().getNamespace().equals(Dialog.MODID))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Dialog.LOGGER.info("在所有 'dialogs' 目录中找到 {} 个JSON文件，其中 {} 个属于模组 {}",
                allFoundResources.size(), modSpecificResources.size(), Dialog.MODID);

        if (isClientContext) {
            sendPlayerMessage(Component.translatable("dialog.manager.files_found", modSpecificResources.size()));
        }
        
        modSpecificResources.forEach((resourceLocation, resource) -> {
            Dialog.LOGGER.info("正在处理对话文件: {}", resourceLocation);
            try {
                DialogSequence sequence = parseDialogSequence(resource, isClientContext); // 解析对话序列
                if (sequence != null && sequence.getId() != null) {
                    dialogSequences.put(sequence.getId(), sequence);
                    Dialog.LOGGER.info("成功加载对话序列: {}", sequence.getId());
                    if (isClientContext) {
                        sendPlayerMessage(Component.translatable("dialog.manager.sequence_loaded", sequence.getId()));
                    }
                } else {
                    Dialog.LOGGER.warn("对话序列为空或ID为空: {}", resourceLocation);
                    if (isClientContext) {
                        sendPlayerMessage(Component.translatable("dialog.manager.sequence_empty", resourceLocation));
                    }
                }
            } catch (Exception e) {
                Dialog.LOGGER.error("加载对话文件失败 {}: {}", resourceLocation, e.getMessage());
                if (isClientContext) {
                    sendPlayerMessage(Component.translatable("dialog.manager.load_failed", resourceLocation, e.getMessage()));
                }
                e.printStackTrace();
            }
        });
        
        Dialog.LOGGER.info("总共加载了 {} 个对话序列 (来自模组 {})", dialogSequences.size(), Dialog.MODID);
        if (isClientContext) {
            sendPlayerMessage(Component.translatable("dialog.manager.total_loaded", dialogSequences.size()));
        }
        if (dialogSequences.isEmpty()) {
            Dialog.LOGGER.warn("模组 {} 没有找到任何对话序列，请检查 'assets/{}/dialogs' 路径和文件格式", Dialog.MODID, Dialog.MODID);
            if (isClientContext) {
                sendPlayerMessage(Component.translatable("dialog.manager.no_sequences"));
            }
        }
    }
    
    /**
     * 解析对话序列JSON文件。
     * @param resource 资源文件。
     * @param isClientContext 是否为客户端上下文。
     */
    private DialogSequence parseDialogSequence(Resource resource, boolean isClientContext) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            // 读取JSON内容用于调试
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            // 重新创建reader以进行解析
            BufferedReader parseReader = new BufferedReader(
                new InputStreamReader(resource.open(), StandardCharsets.UTF_8));
            
            try {
                DialogSequence sequence = GSON.fromJson(parseReader, DialogSequence.class);
                if (sequence == null) {
                    Dialog.LOGGER.error("解析对话JSON失败: 结果为null");
                    Dialog.LOGGER.debug("JSON内容: {}", jsonContent.toString());
                    if (isClientContext) {
                        sendPlayerMessage(Component.translatable("dialog.manager.parse_null"));
                    }
                }
                return sequence;
            } catch (Exception e) {
                Dialog.LOGGER.error("解析对话JSON失败: {}", e.getMessage());
                Dialog.LOGGER.debug("JSON内容: {}", jsonContent.toString());
                if (isClientContext) {
                    sendPlayerMessage(Component.translatable("dialog.manager.parse_failed", e.getMessage()));
                }
                e.printStackTrace();
                return null;
            }
        } catch (IOException e) {
            Dialog.LOGGER.error("读取对话JSON文件失败: {}", e.getMessage());
            if (isClientContext) {
                sendPlayerMessage(Component.translatable("dialog.manager.read_failed", e.getMessage()));
            }
            e.printStackTrace();
            return null;
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
        return new ArrayList<>(dialogHistory);
    }

    /**
     * 记录玩家在当前对话中选择的选项。
     * @param optionText 所选选项的文本。
     */
    @OnlyIn(Dist.CLIENT)
    public void recordChoiceForCurrentDialog(String optionText) {
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
     * 清空对话历史记录。
     */
    @OnlyIn(Dist.CLIENT)
    private void clearDialogHistory() {
        dialogHistory.clear();
    }

    /**
     * 根据ID获取对话序列。
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
    public void showDialog(String dialogId) {
        stopAutoPlay(); // 每次对话启动时重置自动播放为关闭状态
        DialogSequence sequence = getDialogSequence(dialogId);
        if (sequence == null) {
            Dialog.LOGGER.error("Dialog sequence not found: {}", dialogId);
            sendPlayerMessage(Component.translatable("dialog.manager.sequence_not_found", dialogId));
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
        // 更新对话界面
        Minecraft.getInstance().setScreen(new DialogScreen(currentSequence, currentEntry));
    }
}