package top.yourzi.dialog.model;

import com.google.gson.annotations.SerializedName;

/**
 * 表示要在对话中显示的单个物品的信息。
 */
public class DisplayItemInfo {
    // 物品的注册表名称，例如 "minecraft:stone"
    @SerializedName("item")
    private String itemId;

    // 物品数量，默认为1
    @SerializedName("count")
    private int count = 1;

    // 物品的NBT数据，以字符串形式表示的JSON对象
    @SerializedName("nbt")
    private String nbt;

    public DisplayItemInfo() {}

    public DisplayItemInfo(String itemId, int count, String nbt) {
        this.itemId = itemId;
        this.count = count;
        this.nbt = nbt;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getNbt() {
        return nbt;
    }

    public void setNbt(String nbt) {
        this.nbt = nbt;
    }
}