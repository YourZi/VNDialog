*****
# VNDialog
![Mod Status](https://img.shields.io/badge/Status-Developing-yellow) 
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-blueviolet)
![License](https://img.shields.io/badge/License-MIT-blue)

> A lightweight mod that brings a visual novel-style dialogue experience to Minecraft.

---

## 🌟 Introduction
**VNDialog** is a dialogue engine mod designed for Minecraft, inspired by the dialogue systems of classic Galgames. With simple JSON configuration, you can achieve:
- 🎭 Display of multiple character portraits with simple entrance animations
- 💬 Branching dialogue options
- 🎨 Dialogue-triggered commands and interaction with in-game events

Supports hot-loading dialogue configurations via datapacks, allowing for the creation of rich story content without core mod modifications.

---

## 🛠️ Customization Tutorial

### 🖼️ Importing Portraits
   **Resource Pack Configuration**
   Place portrait image files in the `assets/dialog/textures/portraits` path of your resource pack.
   
   ▶  Portrait dimensions are not limited (but try not to make them too large). The mod will automatically adjust the portrait height to start from the bottom of the window and occupy 0.7 times the window height, adapting to various window sizes.

   ▶  It is recommended to use .png format.


### 💬 Dialogue Creation
   **A simplest dialogue is as follows:**
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
Place this json file in the `data/dialog/dialogs` folder of your datapack.

Then, use the `/dialog reload` command in-game to reload the dialogue list. If everything goes well, you should see a message indicating your dialogue has been loaded.
Finally, use the `/dialog show <dialog_id>` command to display your dialogue, for example, `/dialog show hello_world`.

### 📖 Detailed Structure of Dialogue JSON Files

A complete dialogue JSON file contains the following main fields:

```json
{
  "id": "your_dialog_id",
  "title": "Your Dialog Title",
  "description": "A brief description of your dialog.",
  "start": "entry_id_to_start_with",
  "entries": [
    // ... list of dialogue entries ...
  ]
}
```

- **`id` (Required)**: `String`
  - The unique identifier for the dialogue. Used to reference this dialogue in commands, e.g., `/dialog show your_dialog_id`.
- **`title` (Optional)**: `String`
  - The title of the dialogue. If provided, it will be displayed in the list called by commands, for debugging purposes.
- **`description` (Optional)**: `String`
  - A short description of the dialogue. Mainly for developers to understand the dialogue content, not displayed in-game.
- **`start` (Required)**: `String`
  - Specifies the `id` of the first entry to be displayed when the dialogue starts.
- **`entries` (Required)**:
  - An array containing all dialogue entry objects. Each entry represents a screen in the dialogue.

### 💬 Dialogue Entries

Each dialogue entry defines a segment of the dialogue, including who is speaking, what they are saying, which portraits to display, etc. They will play in order unless specified otherwise.

```json
{
  "id": "unique_entry_id",
  "speaker": "Speaker Name",
  "text": "Dialog text.",
  "next": "entry_id_to_go_to_after_this_entry",
  "portraits": [
    // ... list of portrait info ...
  ],
  "options": [
    // ... list of dialogue options ...
  ],
  "commands": [
    // ... list of commands to execute when this entry is displayed ...
  ],
  "display_items": [
    //... list of items to display...
  ]
}
```

- **`id` (Required)**: `String`
  - The unique identifier for the entry. Used for referencing in the `start` field, `next` field, or an option's `target`.
- **`speaker` (Optional)**: `String` or `Text Component`
  - The name or information of the speaker. Can be a plain string or a text component.
- **`text` (Required)**: `String` or `Text Component`
  - The main content of the dialogue. Can be a plain string or a text component.
- **`next` (Optional)**: `String`
  - The `id` of the next entry the dialogue will jump to after the player completes this entry.
- **`portraits` (Optional)**:
  - Defines the portraits displayed in this entry.
- **`options` (Optional)**:
  - Choices offered to the player. If options exist, the dialogue will pause, waiting for the player's selection.
- **`commands` (Optional)**:
  - An array of strings, where each string is a Minecraft command (without the leading `/`). These commands are executed as the player who initiated the dialogue (ignoring original permissions, forcing OP permissions) when this dialogue entry ends.
- **`display_items` (Optional)**:
  - A list of items to display in the dialogue.

### 🎨 Portraits

A portrait object defines how to display a character image in the dialogue interface.

```json
{
  "path": "character_sprite.png",
  "position": "LEFT",
  "brightness": 1.0,
  "animationType": "FADE_IN"
}
```

- **`path` (Required)**: `String`
  - The path to the portrait image. The path is relative to `assets/dialog/textures/portraits/`.
  - For example, `"tlipoca.png"` would load `assets/dialog/textures/portraits/tlipoca.png`.
- **`position` (Optional, defaults to `RIGHT`)**: `String`
  - The position of the portrait on the screen. Possible values:
    - `"LEFT"`: Left side
    - `"RIGHT"`: Right side
    - `"CENTER"`: Center
- **`brightness` (Optional, defaults to `1.0`)**: `Number`
  - The brightness of the portrait. Ranges from `0.0` (completely darkened, pure black silhouette) to `1.0` (normal brightness).
  - Often used to dim portraits of non-current speakers, e.g., `0.5`.
- **`animationType` (Optional, defaults to `NONE`)**: `String`
  - The animation effect when the portrait appears. Common values include:
    - `"NONE"`: No animation, displays immediately.
    - `"FADE_IN"`: Fade-in effect.
    - `"SLIDE_IN_FROM_BOTTOM"`: Slides in from the bottom.
    - `"BOUNCE"`: Bouncing effect (simulates a character being startled).

### ❓ Dialogue Options

Dialogue options allow players to make choices, guiding the dialogue to different branches.

```json
{
  "text": "Choose this option!",
  "target": "entry_id_after_choosing_this",
  "commands": [
    // ... list of commands to execute when this option is chosen ...
  ],
  "visibility_command": "execute if entity @s[tag=test_tag]"
}
```

- **`text` (Required)**: `String` or `Text Component`
  - The text displayed on the option button. Can be a plain string or a text component.
- **`target` (Required)**: `String`
  - The `id` of the entry the dialogue will jump to after the player chooses this option.
- **`commands` (Optional)**: `String`
  - An array of strings, where each string is a Minecraft command (without the leading `/`). These commands are executed after this option is chosen and before jumping to the `target`.
- **`visibility_command` (Optional)**: `String`
  - A Minecraft command string (without the leading `/`). This command is executed as the player who initiated the dialogue (ignoring original permissions, forcing OP permissions) before attempting to display this option. If the command executes successfully and returns `1` (representing true), this option is visible to the player. If the command does not exist, fails to execute, or returns a value other than `1`, this option is not visible to the player.

### 🎁 Item Display

You can display a list of items in the dialogue, used for quests or guidance.

**Significant changes to item stack components in 1.21.1 mean this mod requires time to adapt, so this section is temporarily not applicable to the 1.21.1 version of the mod.**

```json
{
  "item": "minecraft:apple",
  "count": 1,
  "nbt": "{Enchantments:[{lvl: 255s, id: \"minecraft:sharpness\"}]}",
}
```
- **`item` (Required)**:
  - The namespace ID and path of the item. For example, `"minecraft:apple"` represents an apple.
- **`count` (Optional, defaults to `1`)**: `Integer`
  - The quantity of the item.
- **`nbt` (Optional)**: `String`
  - The NBT data of the item. The NBT in the example adds Sharpness 255 enchantment to the item.

### 🔣 Placeholders

You can freely use placeholders in speaker names, dialogue text, and options. Currently available placeholders are:

- **`@i`**: The name of the player currently in the dialogue.


### 📝 Example: A Dialogue with Branches and Portraits
- [Click to jump to the example file](src/main/resources/data/dialog/dialogs/test_dialog.json)
- You can use `/dialog show test_dialog` in-game to preview the effect.

If using translation keys, ensure your translation keys (e.g., `dialog.complex.title`) are defined in the corresponding language file (e.g., `assets/dialog/lang/en_us.json`).

### 💡 Notes

- **ID Uniqueness**: Ensure all dialogue `id`s and entry `id`s are unique within their scope.
- **Resource Paths**: All image paths are relative to specific folders (e.g., `textures/portraits/`). Ensure the resource pack structure is correct.
- **Testing**: Use `/dialog reload` to hot-reload the dialogue list, and `/dialog show <id>` to test your dialogues.


With the tutorial above, you should be able to create rich and diverse dialogue content!



## 📜 License
[MIT License](LICENSE)

*****