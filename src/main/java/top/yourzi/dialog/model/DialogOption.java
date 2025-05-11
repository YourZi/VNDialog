package top.yourzi.dialog.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import net.minecraft.network.chat.Component;


/**
 * 表示对话中的选项。
 */
public class DialogOption {
    // 选项显示的文本，可以是字符串或文本组件JSON对象
    private JsonElement text;
    // 选择此选项后跳转到的对话ID
    @SerializedName("target")
    private String targetId;

    // 缓存的文本组件
    private transient Component cachedTextComponent;
    
    public DialogOption() {
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
        cachedTextComponent = Component.empty();
        return cachedTextComponent;
    }

    public void setText(JsonElement text) {
        this.text = text;
        this.cachedTextComponent = null; // 重置缓存
    }
    
    public String getTargetId() {
        return targetId;
    }
    
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    // 选择该选项后执行的命令
    private String command;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}