package top.yourzi.dialog.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PortraitInfo {
    @SerializedName("path")
    private String path; // 图片路径

    @SerializedName("position")
    private PortraitPosition position = PortraitPosition.RIGHT; // 立绘显示位置

    @SerializedName("brightness")
    private float brightness = 1.0f; // 立绘亮度

    @SerializedName("animationType")
    private PortraitAnimationType animationType = PortraitAnimationType.NONE; //动画类型

    public PortraitInfo() {
    }

    public PortraitInfo(String path, PortraitPosition position, float brightness, PortraitAnimationType animationType) {
        this.path = path;
        this.position = position;
        this.brightness = brightness;
        this.animationType = animationType;
    }

}