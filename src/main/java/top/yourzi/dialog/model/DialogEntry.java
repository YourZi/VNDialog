package top.yourzi.dialog.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import net.minecraft.network.chat.Component;

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
    private java.util.List<PortraitInfo> portraits;
    // 背景图片路径
    private String background;
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
    
    public DialogEntry() {
    }
    
    public Component getText() {
        if (text == null) {
            return Component.empty();
        }
        if (text.isJsonPrimitive() && text.getAsJsonPrimitive().isString()) {
            return Component.literal(text.getAsString());
        }
        if (text.isJsonObject()) {
            return Component.Serializer.fromJson(text);
        }
        if (text.isJsonArray()) {
            net.minecraft.network.chat.MutableComponent combinedText = Component.empty();
            com.google.gson.JsonArray jsonArray = text.getAsJsonArray();
            for (com.google.gson.JsonElement element : jsonArray) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    combinedText.append(Component.literal(element.getAsString()));
                } else if (element.isJsonObject()) {
                    Component component = Component.Serializer.fromJson(element);
                    if (component != null) {
                        combinedText.append(component);
                    }
                }
            }
            return combinedText;
        }
        return Component.empty();
    }

    public void setText(JsonElement text) {
        this.text = text;
    }

    public Component getSpeaker() {
        if (speaker == null) {
            return null; // 或 Component.empty(), 具体取决于业务逻辑
        }
        if (speaker.isJsonPrimitive() && speaker.getAsJsonPrimitive().isString()) {
            return Component.literal(speaker.getAsString());
        }
        if (speaker.isJsonObject()) {
            return Component.Serializer.fromJson(speaker);
        }
        return null; // 或 Component.empty()
    }

    public void setSpeaker(JsonElement speaker) {
        this.speaker = speaker;
    }
    
    public java.util.List<PortraitInfo> getPortraits() {
        return portraits;
    }

    public void setPortraits(java.util.List<PortraitInfo> portraits) {
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

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }
}