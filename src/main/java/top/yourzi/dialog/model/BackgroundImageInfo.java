package top.yourzi.dialog.model;

/**
 * 封装背景图片的路径和渲染选项。
 */
public class BackgroundImageInfo {
    private String path; // 图片路径
    private BackgroundRenderOption renderOption; // 渲染选项

    public BackgroundImageInfo(String path, BackgroundRenderOption renderOption) {
        this.path = path;
        this.renderOption = renderOption;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public BackgroundRenderOption getRenderOption() {
        return renderOption;
    }

    public void setRenderOption(BackgroundRenderOption renderOption) {
        this.renderOption = renderOption;
    }
}