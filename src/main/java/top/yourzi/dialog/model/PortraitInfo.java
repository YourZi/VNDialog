package top.yourzi.dialog.model;

import com.google.gson.annotations.SerializedName;

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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public PortraitPosition getPosition() {
        return position;
    }

    public void setPosition(PortraitPosition position) {
        this.position = position;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public PortraitAnimationType getAnimationType() {
        return animationType;
    }

    public void setAnimationType(PortraitAnimationType animationType) {
        this.animationType = animationType;
    }
}