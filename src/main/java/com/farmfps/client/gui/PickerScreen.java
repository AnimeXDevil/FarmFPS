package com.farmfps.client.gui;

import com.farmfps.FarmFPS;
import com.farmfps.client.HideManager;
import com.farmfps.core.FarmConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Search-as-you-type picker: type a few letters and every matching item (or mob) is listed below;
 * tap an entry to add or remove it from the hidden list. Scroll with the arrow buttons or the
 * mouse wheel. Names starting with the typed text come first.
 */
public class PickerScreen extends Screen {
    public enum Kind { ITEMS, MOBS, STORAGE }

    private static List<String> itemIds;
    private static List<String> mobIds;

    private final Screen parent;
    private final Kind kind;
    private final List<String> all = new ArrayList<>();
    private final List<String> filtered = new ArrayList<>();
    private ButtonWidget[] rowButtons = new ButtonWidget[0];
    private TextFieldWidget search;
    private TextWidget status;
    private int scroll;
    private String query = "";

    public PickerScreen(Screen parent, Kind kind) {
        super(Text.literal(kind == Kind.ITEMS ? "Hide items / blocks" : (kind == Kind.MOBS ? "Hide mobs" : "Hide storage blocks")));
        this.parent = parent;
        this.kind = kind;
    }

    private List<String> selected() {
        FarmConfig c = FarmConfig.INSTANCE;
        if (this.kind == Kind.ITEMS) {
            if (c.hiddenItems == null) {
                c.hiddenItems = new ArrayList<>();
            }
            return c.hiddenItems;
        }
        if (this.kind == Kind.STORAGE) {
            if (c.hiddenContainers == null) {
                c.hiddenContainers = new ArrayList<>();
            }
            return c.hiddenContainers;
        }
        if (c.hiddenMobs == null) {
            c.hiddenMobs = new ArrayList<>();
        }
        return c.hiddenMobs;
    }

    private static List<String> storageIds;

    /** Blocks that hold items: chests, barrels, shulker boxes, furnaces, hoppers and similar. */
    private static List<String> storageList() {
        if (storageIds != null) {
            return storageIds;
        }
        List<String> out = new ArrayList<>();
        for (net.minecraft.block.Block b : Registries.BLOCK) {
            if (b instanceof net.minecraft.block.ChestBlock
                || b instanceof net.minecraft.block.EnderChestBlock
                || b instanceof net.minecraft.block.ShulkerBoxBlock
                || b instanceof net.minecraft.block.BarrelBlock
                || b instanceof net.minecraft.block.HopperBlock
                || b instanceof net.minecraft.block.DispenserBlock
                || b instanceof net.minecraft.block.AbstractFurnaceBlock
                || b instanceof net.minecraft.block.BrewingStandBlock
                || b instanceof net.minecraft.block.DecoratedPotBlock
                || b instanceof net.minecraft.block.ChiseledBookshelfBlock
                || com.farmfps.client.BlockIds.isCrafter(b)) {
                out.add(Registries.BLOCK.getId(b).toString());
            }
        }
        out.sort(String::compareTo);
        storageIds = out;
        return out;
    }

    private static List<String> idList(boolean items) {
        if (items ? itemIds != null : mobIds != null) {
            return items ? itemIds : mobIds;
        }
        List<String> out = new ArrayList<>();
        if (items) {
            for (net.minecraft.item.Item i : Registries.ITEM) {
                String id = Registries.ITEM.getId(i).toString();
                if (!id.equals("minecraft:air")) {
                    out.add(id);
                }
            }
        } else {
            for (net.minecraft.entity.EntityType<?> t : Registries.ENTITY_TYPE) {
                String id = Registries.ENTITY_TYPE.getId(t).toString();
                if (!id.equals("minecraft:player")) {
                    out.add(id);
                }
            }
        }
        out.sort(String::compareTo);
        if (items) {
            itemIds = out;
        } else {
            mobIds = out;
        }
        return out;
    }

    @Override
    protected void init() {
        this.all.clear();
        this.all.addAll(this.kind == Kind.STORAGE ? storageList() : idList(this.kind == Kind.ITEMS));

        int top = 56;
        int bottom = this.height - 30;
        int rowH = 20;
        int rows = Math.max(3, (bottom - top) / (rowH + 2));
        int listW = Math.max(120, Math.min(300, this.width - 70));
        int x = (this.width - listW - 34) / 2;

        Text heading = this.title;
        int titleW = this.textRenderer.getWidth(heading);
        addDrawableChild(new TextWidget((this.width - titleW) / 2, 6, titleW, 9, heading, this.textRenderer));

        this.search = new TextFieldWidget(this.textRenderer, x, 20, listW + 34, 20, Text.literal("Search"));
        this.search.setMaxLength(48);
        this.search.setChangedListener(text -> {
            this.query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
            this.scroll = 0;
            refilter();
        });
        addDrawableChild(this.search);

        this.status = new TextWidget(x, 43, listW + 34, 9, Text.empty(), this.textRenderer);
        addDrawableChild(this.status);

        this.rowButtons = new ButtonWidget[rows];
        for (int i = 0; i < rows; i++) {
            final int slot = i;
            this.rowButtons[i] = ButtonWidget.builder(Text.empty(), b -> toggle(this.scroll + slot))
                .dimensions(x, top + i * (rowH + 2), listW, rowH).build();
            addDrawableChild(this.rowButtons[i]);
        }

        int half = Math.max(1, rows / 2);
        addDrawableChild(ButtonWidget.builder(Text.literal("^"), b -> scrollBy(-half))
            .dimensions(x + listW + 4, top, 30, rowH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("v"), b -> scrollBy(half))
            .dimensions(x + listW + 4, top + (rowH + 2), 30, rowH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("^^"), b -> scrollBy(-rows * 4))
            .dimensions(x + listW + 4, top + 2 * (rowH + 2), 30, rowH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("vv"), b -> scrollBy(rows * 4))
            .dimensions(x + listW + 4, top + 3 * (rowH + 2), 30, rowH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close())
            .dimensions((this.width - 120) / 2, this.height - 26, 120, 20).build());

        refilter();
    }

    private void refilter() {
        this.filtered.clear();
        if (this.query.isEmpty()) {
            this.filtered.addAll(this.all);
        } else {
            List<String> contains = new ArrayList<>();
            for (String id : this.all) {
                String path = id.substring(id.indexOf(':') + 1);
                if (path.startsWith(this.query) || id.startsWith(this.query)) {
                    this.filtered.add(id);
                } else if (id.contains(this.query)) {
                    contains.add(id);
                }
            }
            this.filtered.addAll(contains);
        }
        refreshRows();
    }

    private void scrollBy(int delta) {
        int max = Math.max(0, this.filtered.size() - this.rowButtons.length);
        this.scroll = Math.max(0, Math.min(max, this.scroll + delta));
        refreshRows();
    }

    private void refreshRows() {
        List<String> sel = selected();
        for (int i = 0; i < this.rowButtons.length; i++) {
            int idx = this.scroll + i;
            ButtonWidget b = this.rowButtons[i];
            if (idx < this.filtered.size()) {
                String id = this.filtered.get(idx);
                String shown = id.startsWith("minecraft:") ? id.substring(10) : id;
                b.visible = true;
                b.setMessage(Text.literal((sel.contains(id) ? "[X] " : "[   ] ") + shown));
            } else {
                b.visible = false;
            }
        }
        if (this.status != null) {
            this.status.setMessage(Text.literal(this.filtered.size() + " matches, " + sel.size()
                + " hidden. Tap an entry to hide / show it."));
        }
    }

    private void toggle(int index) {
        if (index < 0 || index >= this.filtered.size()) {
            return;
        }
        String id = this.filtered.get(index);
        List<String> sel = selected();
        if (!sel.remove(id)) {
            sel.add(id);
        }
        refreshRows();
    }

    /** Mouse wheel; deliberately without @Override so a changed parent signature cannot break the build. */
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollBy(verticalAmount > 0 ? -3 : 3);
        return true;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void removed() {
        FarmFPS.saveConfig();
        if (this.kind == Kind.ITEMS || this.kind == Kind.STORAGE) {
            HideManager.reload();
        } else {
            HideManager.rebuild();
        }
    }
}
