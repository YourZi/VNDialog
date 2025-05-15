package top.yourzi.dialog.datagen.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;
import top.yourzi.dialog.Dialog;
import top.yourzi.dialog.model.DialogEntry;
import top.yourzi.dialog.model.DialogSequence;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

public abstract class DialogProvider implements DataProvider {
    protected final PackOutput output;
    protected static final ArrayList<DialogSequence> dialogSequences = new ArrayList<>();

    public DialogProvider(PackOutput output) {
        this.output = output;
    }

    protected abstract void registerDialogs();

    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cache) {
        this.clearDialogs();
        this.registerDialogs();
        return this.generateAllDialogs(cache);
    }

    protected CompletableFuture<?> generateAllDialogs(CachedOutput cache) {
        CompletableFuture<?>[] futures = new CompletableFuture[dialogSequences.size()];
        int i = 0;

        DialogSequence dialog;
        Path target;
        for(Iterator<DialogSequence> iterator = dialogSequences.iterator();
            iterator.hasNext();
            futures[i++] = DataProvider.saveStable(cache, serializeDialog(dialog), target)) {

            dialog = iterator.next();
            target = this.getPath(dialog);
        }

        return CompletableFuture.allOf(futures);
    }

    // 辅助方法
    private JsonElement serializeDialog(DialogSequence dialog) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJsonTree(dialog);
    }

    protected void clearDialogs() {
        this.dialogSequences.clear();
    }

    protected Path getPath(DialogSequence dialogSequence) {
        return this.output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(Dialog.MODID).resolve("dialogs").resolve(dialogSequence.getId() + ".json");
    }

    @Override
    public @NotNull String getName() {
        return "Dialog Data Provider";
    }

    public static class dialogBuilder{
        public static dialogBuilder builder() {
            return new dialogBuilder();
        }

        private String id;
        private String title;
        private String description;
        private ArrayList<DialogEntry> entries = new ArrayList<>();
        private String startId;

        public dialogBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public dialogBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public dialogBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public dialogBuilder addEntry(DialogEntry entry) {
            this.entries.add(entry);
            return this;
        }

        public void build(String startId) {
            DialogSequence dialogSequence = new DialogSequence();
            dialogSequence.setId(id);
            dialogSequence.setTitle(title);
            dialogSequence.setDescription(description);
            dialogSequence.setEntries(entries.toArray(new DialogEntry[0]));
            dialogSequence.setStartId(startId);
            dialogSequences.add(dialogSequence);
        }
    }
}
