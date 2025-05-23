package top.yourzi.dialog.datagen;

import com.google.gson.JsonElement;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Items;
import top.yourzi.dialog.datagen.provider.DialogProvider;
import top.yourzi.dialog.model.*;

import java.awt.*;
import java.util.List;

public class DialogGenerator extends DialogProvider {
    public DialogGenerator(PackOutput output) {
        super(output);
    }

    private static JsonElement literalText(String text) {
        return Component.Serializer.toJsonTree(Component.literal(text));
    }

    private static JsonElement literalText(String text, Style style) {
        return Component.Serializer.toJsonTree(Component.literal(text).withStyle(style));
    }

    @Override
    protected void registerDialogs() {
        dialogBuilder.builder().setId("example_dialog")
                .setTitle("Example Dialog")
                .setDescription("This is an example dialog.")
                .addEntry(
                        DialogEntry.builder()
                                .id("1")
                                .nextId("2")
                                .text(literalText("Hello, how are you?", Style.EMPTY.withBold(true).withColor(Color.ORANGE.getRGB())))
                                .speaker(literalText("NPC"))
                                .portraits(List.of(new PortraitInfo("leaf.png", PortraitPosition.LEFT, 1.0f, PortraitAnimationType.NONE)))
                                .displayItems(List.of(new DisplayItemInfo(Items.ACACIA_FENCE.getDescriptionId(), 1, "")))
                                .backgroundImage(new BackgroundImageInfo("example_background.png", BackgroundRenderOption.FILL))
                                .build())
                .addEntry(
                        DialogEntry.builder()
                                .id("2")
                                .nextId("3")
                                .text(literalText("I'm fine, thank you!"))
                                .speaker(literalText("Player"))
                                .options(new DialogOption[]{
                                        DialogOption.builder()
                                                .targetId("end")
                                                .text(literalText("Clear"))
                                                .command("weather clear")
                                                .build(),
                                        DialogOption.builder()
                                                .text(literalText("Rain"))
                                                .targetId("end")
                                                .build()})
                                .build())
                .addEntry(
                        DialogEntry.builder()
                                .id("end")
                                .speaker(literalText("NPC"))
                                .build())
                .build("1");
    }
}
