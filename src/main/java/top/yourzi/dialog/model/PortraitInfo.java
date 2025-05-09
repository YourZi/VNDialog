package top.yourzi.dialog.model;

import com.google.gson.annotations.SerializedName;

public class PortraitInfo {
    @SerializedName("path")
    private String path; // 图片路径（通常是文件名）

    @SerializedName("position")
    private PortraitPosition position = PortraitPosition.RIGHT; // 立绘显示位置

    @SerializedName("brightness")
    private float brightness = 1.0f; // 立绘亮度 (1.0为正常亮度)

    @SerializedName("animation")
    private String animation_legacy = "none"; // 旧版动画效果字段（字符串类型），保留以兼容旧配置

    @SerializedName("animationType")
    private PortraitAnimationType animationType = PortraitAnimationType.NONE; // 新版动画类型 (枚举类型)

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
        // 兼容旧的 animation 字符串字段
        if (this.animationType == PortraitAnimationType.NONE && this.animation_legacy != null && !this.animation_legacy.equalsIgnoreCase("none")) {
            try {
                return PortraitAnimationType.valueOf(this.animation_legacy.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 若旧的 animation 字符串无法转换为枚举，则返回 NONE
                return PortraitAnimationType.NONE;
            }
        }
        return animationType;
    }

    public void setAnimationType(PortraitAnimationType animationType) {
        this.animationType = animationType;
        this.animation_legacy = null; // 设置新的 animationType 时，清除旧的 animation_legacy 字符串，以避免混淆
    }

    /**
     * 获取旧版动画效果字符串。
     * @deprecated 建议使用 {@link #getAnimationType()} 和 {@link PortraitAnimationType} 枚举。
     */
    @Deprecated
    public String getAnimation() {
        return animation_legacy;
    }

    /**
     * 设置旧版动画效果字符串，并尝试转换为新版枚举类型。
     * @deprecated 建议使用 {@link #setAnimationType(PortraitAnimationType)} 和 {@link PortraitAnimationType} 枚举。
     */
    @Deprecated
    public void setAnimation(String animation) {
        this.animation_legacy = animation;
        // 尝试将旧的 animation 字符串转换为新的枚举类型
        if (animation != null && !animation.isEmpty()) {
            try {
                this.animationType = PortraitAnimationType.valueOf(animation.toUpperCase());
            } catch (IllegalArgumentException e) {
                this.animationType = PortraitAnimationType.NONE; // 若转换失败，则默认为 NONE
            }
        } else {
            this.animationType = PortraitAnimationType.NONE;
        }
    }
}