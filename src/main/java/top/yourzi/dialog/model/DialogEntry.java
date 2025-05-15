package top.yourzi.dialog.model;

import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Map;
import java.util.ArrayList;

/**
 * 表示单条对话的数据模型。
 */
@Setter
@Getter
@Builder
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

    // 需要在对话中显示的物品列表
    @SerializedName("display_items")
    private List<DisplayItemInfo> displayItems;

    public Component placeHolderReplace(String fromString, String toString, JsonElement targetElement) {
        if (targetElement == null || targetElement.isJsonNull()) {
            return Component.empty();
        }
        String pString = (toString == null) ? "" : toString;

        if (targetElement.isJsonObject()) {
            JsonObject jsonObjectCopy = targetElement.getAsJsonObject().deepCopy();
            performDeepPlaceholderReplace(jsonObjectCopy, fromString, pString);

            Component componentAfterJsonProcessing;
            try {
                componentAfterJsonProcessing = Component.Serializer.fromJson(jsonObjectCopy);
            } catch (JsonSyntaxException e) {
                try {
                    componentAfterJsonProcessing = Component.Serializer.fromJson(targetElement);
                } catch (JsonSyntaxException e2) {
                    return Component.empty();
                }
            }
            return replaceTextInComponent(componentAfterJsonProcessing, fromString, pString);

        } else if (targetElement.isJsonArray()) {
            MutableComponent combinedText = Component.empty();
            JsonArray jsonArray = targetElement.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                combinedText.append(placeHolderReplace(fromString, pString, element));
            }
            return combinedText;
        } else if (targetElement.isJsonPrimitive() && targetElement.getAsJsonPrimitive().isString()) {
            return Component.literal(targetElement.getAsString().replace(fromString, pString));
        }
        
        try {
            Component component = Component.Serializer.fromJson(targetElement);
            return replaceTextInComponent(component, fromString, pString);
        } catch (JsonSyntaxException e) {
            return Component.empty();
        }
    }

    private Component replaceTextInComponent(Component component, String placeholder, String replacement) {
        if (component == null) {
            return Component.empty();
        }

        MutableComponent newComponent = Component.empty();
        newComponent.setStyle(component.getStyle());

        component.visit((style, text) -> {
            String replacedText = text.replace(placeholder, replacement);
            newComponent.append(Component.literal(replacedText).setStyle(style));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);

        return newComponent;
    }

    public Component getText(String playerName) {
        return placeHolderReplace("@i", playerName, text);
    }

    public Component getSpeaker(String playerName) {
        return placeHolderReplace("@i", playerName, speaker);
    }

    public boolean hasOptions() {
        return options != null && options.length > 0;
    }

    private boolean performDeepPlaceholderReplace(JsonObject jsonObject, String placeholder, String replacement) {
        boolean modified = false;
        for (Map.Entry<String, JsonElement> entry : new ArrayList<>(jsonObject.entrySet())) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                String originalString = value.getAsString();
                String replacedString = originalString.replace(placeholder, replacement);
                if (!originalString.equals(replacedString)) {
                    jsonObject.addProperty(key, replacedString);
                    modified = true;
                }
            } else if (value.isJsonObject()) {
                if (performDeepPlaceholderReplace(value.getAsJsonObject(), placeholder, replacement)) {
                    modified = true;
                }
            } else if (value.isJsonArray()) {
                JsonArray jsonArray = value.getAsJsonArray();
                if (performDeepPlaceholderReplaceInArray(jsonArray, placeholder, replacement)) {
                    modified = true;
                }
            }
        }
        return modified;
    }

    private boolean performDeepPlaceholderReplaceInArray(JsonArray jsonArray, String placeholder, String replacement) {
        boolean overallArrayModified = false;
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement element = jsonArray.get(i);
            boolean elementModifiedInLoop = false;
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String originalString = element.getAsString();
                String replacedString = originalString.replace(placeholder, replacement);
                if (!originalString.equals(replacedString)) {
                    jsonArray.set(i, new JsonPrimitive(replacedString));
                    elementModifiedInLoop = true;
                }
            } else if (element.isJsonObject()) {
                JsonObject nestedObject = element.getAsJsonObject();
                if (performDeepPlaceholderReplace(nestedObject, placeholder, replacement)) {
                    elementModifiedInLoop = true; 
                }
            } else if (element.isJsonArray()) {
                if (performDeepPlaceholderReplaceInArray(element.getAsJsonArray(), placeholder, replacement)) {
                     elementModifiedInLoop = true;
                }
            }
            if(elementModifiedInLoop) overallArrayModified = true;
        }
        return overallArrayModified;
    }
}