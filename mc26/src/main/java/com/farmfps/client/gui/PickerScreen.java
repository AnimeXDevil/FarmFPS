package com.farmfps.client.gui;

import com.farmfps.FarmFPS;
import com.farmfps.client.HideManager;
import com.farmfps.core.FarmConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.farmfps.client.ScreenOpener;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

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
    private Button[] rowButtons = new Button[0];
    private EditBox search;
    private Component status = Component.empty();
    private int statusX;
    private int scroll;
    private String query = "";

    public PickerScreen(Screen parent, Kind kind) {
        super(Component.literal(kind == Kind.ITEMS ? "Hide items / blocks" : (kind == Kind.MOBS ? "Hide mobs" : "Hide storage blocks")));
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
        for (net.minecraft.world.level.block.Block b : BuiltInRegistries.BLOCK) {
            if (b instanceof net.minecraft.world.level.block.ChestBlock
                || b instanceof net.minecraft.world.level.block.EnderChestBlock
                || b instanceof net.minecraft.world.level.block.ShulkerBoxBlock
                || b instanceof net.minecraft.world.level.block.BarrelBlock
                || b instanceof net.minecraft.world.level.block.HopperBlock
                || b instanceof net.minecraft.world.level.block.DispenserBlock
                || b instanceof net.minecraft.world.level.block.AbstractFurnaceBlock
                || b instanceof net.minecraft.world.level.block.BrewingStandBlock
                || b instanceof net.minecraft.world.level.block.DecoratedPotBlock
                || BuiltInRegistries.BLOCK.getKey(b).getPath().equals("chiseled_bookshelf")
                || com.farmfps.client.BlockIds.isCrafter(b)) {
                out.add(BuiltInRegistries.BLOCK.getKey(b).toString());
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
            for (net.minecraft.world.item.Item i : BuiltInRegistries.ITEM) {
                String id = BuiltInRegistries.ITEM.getKey(i).toString();
                if (!id.equals("minecraft:air")) {
                    out.add(id);
                }
            }
        } else {
            for (net.minecraft.world.entity.EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
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
        this.statusX = x;

        this.search = new EditBox(this.font, x, 20, listW + 34, 20, Component.literal("Search"));
        this.search.setMaxLength(48);
        this.search.setResponder(text -> {
            this.query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
            this.scroll = 0;
            refilter();
        });
        addRenderableWidget(this.search);


        this.rowButtons = new Button[rows];
        for (int i = 0; i < rows; i++) {
            final int slot = i;
            this.rowButtons[i] = Button.builder(Component.empty(), b -> toggle(this.scroll + slot))
                .bounds(x, top + i * (rowH + 2), listW, rowH).build();
            addRenderableWidget(this.rowButtons[i]);
        }

        int half = Math.max(1, rows / 2);
        addRenderableWidget(Button.builder(Component.literal("^"), b -> scrollBy(-half))
            .bounds(x + listW + 4, top, 30, rowH).build());
        addRenderableWidget(Button.builder(Component.literal("v"), b -> scrollBy(half))
            .bounds(x + listW + 4, top + (rowH + 2), 30, rowH).build());
        addRenderableWidget(Button.builder(Component.literal("^^"), b -> scrollBy(-rows * 4))
            .bounds(x + listW + 4, top + 2 * (rowH + 2), 30, rowH).build());
        addRenderableWidget(Button.builder(Component.literal("vv"), b -> scrollBy(rows * 4))
            .bounds(x + listW + 4, top + 3 * (rowH + 2), 30, rowH).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
            .bounds((this.width - 120) / 2, this.height - 26, 120, 20).build());

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
            Button b = this.rowButtons[i];
            if (idx < this.filtered.size()) {
                String id = this.filtered.get(idx);
                String shown = id.startsWith("minecraft:") ? id.substring(10) : id;
                b.visible = true;
                b.setMessage(Component.literal((sel.contains(id) ? "[X] " : "[   ] ") + shown));
            } else {
                b.visible = false;
            }
        }
        {
            this.status = (Component.literal(this.filtered.size() + " matches, " + sel.size()
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
    public void onClose() {
        ScreenOpener.show(this.minecraft, this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.font, this.title, this.width / 2, 6, 0xFFFFFFFF);
        g.text(this.font, this.status, this.statusX, 43, 0xFFAAAAAA);
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
