package top.yourzi.dialog.model;

import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 表示单条对话的数据模型。
 */
public class DialogEntry {
    // 对话文本内容，可以是字符串或文本组件JSON对象
    private JsonElement text;
    // 说话者名称，可以是字符串或文本组件JSON对象
    private JsonElement speaker;
    // 立绘信息列表
    @SerializedName("portraits")
    private List<PortraitInfo> portraits;
    // 对话ID，用于跳转
    private String id;
    // 下一条对话的ID，如果为空则按顺序显示下一条
    @SerializedName("next")
    private String nextId;
    // 可选的对话选项
    private DialogOption[] options;
    // 用户选择的选项文本
    private String selectedOptionText;
    // 该对话条目完成后执行的命令
    private String command;

    // 缓存的文本组件
    private transient Component cachedTextComponent;
    // 缓存的说话者组件
    private transient Component cachedSpeakerComponent;
    
    public DialogEntry() {
    }
    
    public Component getText() {
        if (cachedTextComponent != null) {
            return cachedTextComponent;
        }
        if (text == null) {
            cachedTextComponent = Component.empty();
            return cachedTextComponent;
        }
        if (text.isJsonPrimitive() && text.getAsJsonPrimitive().isString()) {
            cachedTextComponent = Component.literal(text.getAsString());
            return cachedTextComponent;
        }
        if (text.isJsonObject()) {
            cachedTextComponent = Component.Serializer.fromJson(text);
            return cachedTextComponent;
        }
        if (text.isJsonArray()) {
            MutableComponent combinedText = Component.empty();
            JsonArray jsonArray = text.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    combinedText.append(Component.literal(element.getAsString()));
                } else if (element.isJsonObject()) {
                    Component component = Component.Serializer.fromJson(element);
                    if (component != null) {
                        combinedText.append(component);
                    }
                }
            }
            cachedTextComponent = combinedText;
            return cachedTextComponent;
        }
        cachedTextComponent = Component.empty();
        return cachedTextComponent;
    }

    public void setText(JsonElement text) {
        this.text = text;
        this.cachedTextComponent = null; // 重置缓存
    }

    public Component getSpeaker() {
        if (cachedSpeakerComponent != null) {
            return cachedSpeakerComponent;
        }
        if (speaker == null) {
            return null;
        }
        if (speaker.isJsonPrimitive() && speaker.getAsJsonPrimitive().isString()) {
            cachedSpeakerComponent = Component.literal(speaker.getAsString());
            return cachedSpeakerComponent;
        }
        if (speaker.isJsonObject()) {
            cachedSpeakerComponent = Component.Serializer.fromJson(speaker);
            return cachedSpeakerComponent;
        }
        return null;
    }

    public void setSpeaker(JsonElement speaker) {
        this.speaker = speaker;
        this.cachedSpeakerComponent = null; // 重置缓存
    }
    
    public List<PortraitInfo> getPortraits() {
        return portraits;
    }

    public void setPortraits(List<PortraitInfo> portraits) {
        this.portraits = portraits;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getNextId() {
        return nextId;
    }
    
    public void setNextId(String nextId) {
        this.nextId = nextId;
    }
    
    public DialogOption[] getOptions() {
        return options;
    }
    
    public void setOptions(DialogOption[] options) {
        this.options = options;
    }
    
    public boolean hasOptions() {
        return options != null && options.length > 0;
    }

    public String getSelectedOptionText() {
        return selectedOptionText;
    }

    public void setSelectedOptionText(String selectedOptionText) {
        this.selectedOptionText = selectedOptionText;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}