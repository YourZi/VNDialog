package top.yourzi.dialog.model;

import com.google.gson.annotations.SerializedName;

/**
 * 存储对话中提交物品的相关信息。
 */
public class SubmitItemInfo {
    @SerializedName("item_id")
    private String itemId;

    @SerializedName("item_nbt")
    private String itemNbt; // 可选

    @SerializedName("item_count")
    private int itemCount = 1; // 可选，默认为1

    @SerializedName("target_dialog_id")
    private String targetDialogId;

    @SerializedName("command")
    private String command; // 可选

    public SubmitItemInfo() {
    }

    public SubmitItemInfo(String itemId, String itemNbt, int itemCount, String targetDialogId, String command) {
        this.itemId = itemId;
        this.itemNbt = itemNbt;
        this.itemCount = itemCount > 0 ? itemCount : 1; // 确保数量至少为1
        this.targetDialogId = targetDialogId;
        this.command = command;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemNbt() {
        return itemNbt;
    }

    public void setItemNbt(String itemNbt) {
        this.itemNbt = itemNbt;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount > 0 ? itemCount : 1;
    }

    public String getTargetDialogId() {
        return targetDialogId;
    }

    public void setTargetDialogId(String targetDialogId) {
        this.targetDialogId = targetDialogId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}