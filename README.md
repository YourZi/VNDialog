*****
# VNDialog/VN对话引擎
![Mod Status](https://img.shields.io/badge/Status-Developing-yellow) 
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-blueviolet)
![License](https://img.shields.io/badge/License-MIT-blue)

> 为Minecraft带来视觉小说式对话体验的轻量化模组

---

## 🌟 简介  
**VNDialog** 是一个为Minecraft设计的对话引擎模组，灵感来源于经典Galgame的对话系统。通过简单的JSON配置即可实现：  
- 🎭 多角色立绘显示与简单的入场动画  
- 💬 分支对话选项  
- 🎨 对话触发指令与游戏内事件互动

支持通过数据包热加载对话配置，无需魔改即可创建丰富的剧情内容。

---

## 🛠️ 自定义教程

### 🖼️ 立绘导入
   **资源包配置**  
   在资源包的`assets/dialog/textures/portraits`路径下放置立绘图片文件  
   
   ▶  立绘尺寸不限(但尽量不要过大)，模组会自动调整立绘高度，使其从窗口底部开始，占据窗口高度的0.7倍，以适应各种尺寸的窗口

   ▶  建议采用.png格式


### 💬对话制作
   **一个最简单的对话如下所示:**
```json
{
  "id": "hello_world",
  "title": "Hello World",
  "description": "A simple dialog",
  "start": "start",
  "entries": [
    {
      "id": "start",
      "speaker": "System",
      "text": "Hello world! ",
      "portraits":[
       {"path": "character.png"} 
      ]           
    }
  ]
}
```
将这个json文件放在数据包的`data/dialog/dialogs`文件夹下。

然后，在游戏内使用`/dialog reload`指令重载对话列表。如果一切顺利，你应该能看到你的对话被加载的提示。
最后，使用`/dialog show <dialog_id>`指令来展示你的对话，例如`/dialog show hello_world`。

### 📖 对话JSON文件结构详解

一个完整的对话JSON文件包含以下主要字段：

```json
{
  "id": "your_dialog_id",
  "title": "Your Dialog Title",
  "description": "A brief description of your dialog.",
  "start": "entry_id_to_start_with",
  "entries": [
    // ... 对话条目列表 ...
  ]
}
```

- **`id` (必需)**: `String`
  - 对话的唯一标识符。用于在指令中引用此对话，例如 `/dialog show your_dialog_id`。
- **`title` (可选)**: `String`
  - 对话的标题。如果提供，它会显示在指令调用出的列表里，调试使用。
- **`description` (可选)**: `String`
  - 对话的简短描述。主要用于开发者理解对话内容，不在游戏中显示。
- **`start` (必需)**: `String`
  - 指定对话开始时显示的第一个条目的 `id`。
- **`entries` (必需)**:
  - 一个包含所有对话条目对象的数组。每个条目代表对话中的一个界面。

### 💬 对话条目

每个对话条目定义了对话中的一个片段，包括谁在说话、说什么、显示什么立绘等。没有特殊要求的情况下会按顺序播放。

```json
{
  "id": "unique_entry_id",
  "speaker": "Speaker Name",
  "text": "Dialog text.",
  "next": "entry_id_to_go_to_after_this_entry",
  "portraits": [
    // ... 立绘信息列表 ...
  ],
  "options": [
    // ... 对话选项列表 ...
  ],
  "commands": [
    // ... 当此条目显示时执行的指令列表 ...
  ]
}
```

- **`id` (必需)**: `String`
  - 条目的唯一标识符。用于在 `start` 字段、`next` 字段或选项的 `target` 中引用。
- **`speaker` (可选)**: `String` 或 `Text Component`
  - 说话者的名字或信息。可以是普通字符串或文本组件。
- **`text` (必需)**: `String` 或 `Text Component`
  - 对话的主要内容。可以是普通字符串或文本组件。
- **`next` (可选)**: `String`
  - 当玩家完成此条目的对话后，对话将跳转到的下一个条目的 `id`。
- **`portraits` (可选)**: 
  - 定义此条目中显示的立绘。
- **`options` (可选)**:
  - 提供给玩家的选择。如果存在选项，对话将暂停等待玩家选择。
- **`commands` (可选)**:
  - 一个字符串数组，其中每个字符串都是一个Minecraft指令（不需要前导 `/`）。这些指令会在该对话条目结束时，以发起对话的玩家的身份（忽略原本权限，强制以OP权限）执行。

### 🎨 立绘

立绘对象定义了如何在对话界面中显示角色图片。

```json
{
  "path": "character_sprite.png",
  "position": "LEFT",
  "brightness": 1.0,
  "animationType": "FADE_IN"
}
```

- **`path` (必需)**: `String`
  - 立绘图片的路径。路径相对于 `assets/dialog/textures/portraits/`。
  - 例如，`"tlipoca.png"` 会加载 `assets/dialog/textures/portraits/tlipoca.png`。
- **`position` (可选, 默认为 `RIGHT`)**: `String`
  - 立绘在屏幕上的位置。可选值：
    - `"LEFT"`: 左侧
    - `"RIGHT"`: 右侧
    - `"CENTER"`: 中间
- **`brightness` (可选, 默认为 `1.0`)**: `Number`
  - 立绘的亮度。范围从 `0.0` (完全变暗，纯黑剪影) 到 `1.0` (正常亮度)。
  - 常用于将非当前说话者的立绘变暗，例如 `0.5`。
- **`animationType` (可选, 默认为 `NONE`)**: `String`
  - 立绘出现时的动画效果。常见值包括：
    - `"NONE"`: 无动画，立即显示。
    - `"FADE_IN"`: 淡入效果。
    - `"SLIDE_IN_FROM_BOTTOM"`: 从下方滑入。
    - `"BOUNCE"`: 弹跳（模拟角色受惊吓的效果）。

### ❓ 对话选项

对话选项允许玩家做出选择，从而引导对话走向不同的分支。

```json
{
  "text": "Choose this option!",
  "target": "entry_id_after_choosing_this",
  "commands": [
    // ... 当此选项被选择时执行的指令列表 ...
  ],
  "visibility_command": "execute if entity @s[tag=test_tag]"
}
```

- **`text` (必需)**: `String` 或 `Text Component`
  - 选项按钮上显示的文本。可以是普通字符串或文本组件。
- **`target` (必需)**: `String`
  - 当玩家选择此选项后，对话将跳转到的条目的 `id`。
- **`commands` (可选)**: `String`
  - 一个字符串数组，其中每个字符串都是一个Minecraft指令（不需要前导 `/`）。这些指令会在该选项被选择后、跳转到 `target` 之前执行。
- **`visibility_command` (可选)**: `String`
  - 一个Minecraft指令字符串（不需要前导 `/`）。该指令会在尝试展示此选项前，以发起对话的玩家的身份（忽略原本权限，强制以OP权限）执行。如果指令执行成功并返回值为 `1` (代表true)，则此选项对该玩家可见。如果指令不存在、执行失败或返回值不为 `1`，则此选项对该玩家不可见。

### 🔣占位符

你可以自由地在说话者和对话文本、选项中使用占位符，目前已有的占位符如下：

- **`@i`**:当前对话的玩家名称


### 📝 示例：一个包含分支和立绘的对话
- [点击跳转至示例文件](src/main/resources/data/dialog/dialogs/test_dialog.json)
- 可以在游戏内使用`/dialog show test_dialog`来预览效果

如果有使用翻译键，确保你的翻译键（如 `dialog.complex.title`）在对应的语言文件（例如 `assets/dialog/lang/zh_cn.json`）中有定义。

### 💡 注意事项

- **ID唯一性**: 确保所有对话 `id` 和条目 `id` 在其作用域内是唯一的。
- **资源路径**: 所有图片路径都是相对于特定文件夹的（例如 `textures/portraits/`）。请确保资源包结构正确。
- **测试**: 使用 `/dialog reload` 可以热重载对话列表，用 `/dialog show <id>` 可以测试你的对话。


通过以上教程，你应该能够创建出丰富多样的对话内容了！



## 📜 许可证/License
[MIT License](LICENSE)

*****
