package top.yourzi.dialog.model;

import java.util.ArrayList;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;


/**
 * 表示对话中的选项。
 */
public class DialogOption {
    
    // 选项显示的文本，可以是字符串或文本组件JSON对象
    @Setter
    private JsonElement text;

    // 选择此选项后跳转到的对话ID
    @Getter
    @Setter
    @SerializedName("target")
    private String targetId;
    
    // 选择该选项后执行的命令
    @Getter
    @Setter
    private String command;

    // 控制该选项是否可见的指令
    @Getter
    @Setter
    @SerializedName("visibility_command")
    private String visibilityCommand;

    // 缓存的文本组件
    private transient Component cachedTextComponent;
    
    public DialogOption() {
    }
    
    public Component getText(HolderLookup.Provider provider, String playerName) {
        return placeHolderReplace("@i", playerName, this.text, provider);
    }


    public Component placeHolderReplace(String fromString, String toString, JsonElement targetElement, HolderLookup.Provider provider) {
        if (targetElement == null || targetElement.isJsonNull()) {
            return Component.empty();
        }
        String pString = (toString == null) ? "" : toString;


        if (targetElement.isJsonObject()) {
            JsonObject jsonObjectCopy = targetElement.getAsJsonObject().deepCopy();
            performDeepPlaceholderReplace(jsonObjectCopy, fromString, pString);

            Component componentAfterJsonProcessing;
            try {
                componentAfterJsonProcessing = Component.Serializer.fromJson(jsonObjectCopy, provider);
            } catch (JsonSyntaxException e) {
                try {
                    componentAfterJsonProcessing = Component.Serializer.fromJson(targetElement, provider);
                } catch (JsonSyntaxException e2) {
                    return Component.empty();
                }
            }
            return replaceTextInComponent(componentAfterJsonProcessing, fromString, pString);

        } else if (targetElement.isJsonArray()) {
            MutableComponent combinedText = Component.empty();
            JsonArray jsonArray = targetElement.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                combinedText.append(placeHolderReplace(fromString, pString, element, provider));
            }
            return combinedText;
        } else if (targetElement.isJsonPrimitive() && targetElement.getAsJsonPrimitive().isString()) {
            return Component.literal(targetElement.getAsString().replace(fromString, pString));
        }
        
        try {
            Component component = Component.Serializer.fromJson(targetElement, provider);
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