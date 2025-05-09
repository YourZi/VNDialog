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
    
    public DialogOption() {
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
        return Component.empty();
    }

    public void setText(JsonElement text) {
        this.text = text;
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