package com.farmfps.client.gui;

import com.farmfps.FarmFPS;
import com.farmfps.client.HideManager;
import com.farmfps.core.FarmConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Three-page settings screen with large toggles and sliders, no hover or keyboard needed, so it
 * works with touch controls. Opens from Mod Menu (if installed) or with /farmfps.
 */
public class ConfigScreen extends Screen {
    private static final int PAGES = 4;

    private final Screen parent;
    private final int page;

    public ConfigScreen(Screen parent) {
        this(parent, 0);
    }

    private ConfigScreen(Screen parent, int page) {
        super(Text.literal("FarmFPS Settings"));
        this.parent = parent;
        this.page = page;
    }

    @Override
    protected void init() {
        FarmConfig c = FarmConfig.INSTANCE;

        int gap = 6;
        int colW = Math.max(100, Math.min(160, (this.width - 24 - gap) / 2));
        int left = (this.width - (colW * 2 + gap)) / 2;
        int right = left + colW + gap;
        int rows = this.page == 0 ? 5 : (this.page == 1 ? 5 : (this.page == 2 ? 8 : 8));
        int rowH = Math.max(20, Math.min(26, (this.height - 40) / rows - 4));
        int step = rowH + 4;
        int y = 28;

        Text heading = Text.literal("FarmFPS Settings  (" + (this.page + 1) + "/" + PAGES + ")");
        int titleW = this.textRenderer.getWidth(heading);
        addDrawableChild(new TextWidget((this.width - titleW) / 2, 10, titleW, 9, heading, this.textRenderer));

        if (this.page == 0) {
            toggle(left, y, colW, rowH, "Mod", () -> c.enabled, v -> c.enabled = v);
            addDrawableChild(ButtonWidget.builder(presetText(c), b -> {
                c.applyPreset((c.preset + 1) % FarmConfig.PRESET_NAMES.length);
                b.setMessage(presetText(c));
            }).dimensions(right, y, colW, rowH).build());
            y += step;
            toggle(left, y, colW, rowH, "Cap items", () -> c.capItems, v -> c.capItems = v);
            toggle(right, y, colW, rowH, "Cap XP orbs", () -> c.capXpOrbs, v -> c.capXpOrbs = v);
            y += step;
            toggle(left, y, colW, rowH, "Drop extra items", () -> c.aggressiveItems, v -> c.aggressiveItems = v);
            toggle(right, y, colW, rowH, "Hide farm fx", () -> c.hideFarmEffects, v -> c.hideFarmEffects = v);
            y += step;
            toggle(left, y, colW, rowH, "Fx budgets", () -> c.throttleParticles && c.throttleSounds,
                v -> { c.throttleParticles = v; c.throttleSounds = v; });
            toggle(right, y, colW, rowH, "Action bar", () -> c.actionBar, v -> c.actionBar = v);
            y += step;
        } else if (this.page == 1) {
            cycle(left, y, colW, rowH, "Scope", FarmConfig.SCOPE_NAMES, () -> c.redstoneScope, v -> c.redstoneScope = v);
            cycle(right, y, colW, rowH, "Redstone", FarmConfig.REDSTONE_NAMES, () -> c.redstoneMode, v -> c.redstoneMode = v);
            y += step;
            cycle(left, y, colW, rowH, "Pistons", FarmConfig.PISTON_NAMES, () -> c.pistonMode, v -> c.pistonMode = v);
            toggle(right, y, colW, rowH, "Skip light", () -> c.skipLightUpdates, v -> c.skipLightUpdates = v);
            y += step;
            toggle(left, y, colW, rowH, "Mute redstone snd", () -> c.muteRedstoneSounds, v -> c.muteRedstoneSounds = v);
            toggle(right, y, colW, rowH, "Cull far blocks", () -> c.blockEntityCull, v -> c.blockEntityCull = v);
            y += step;
            toggle(left, y, colW, rowH, "Cull far mobs", () -> c.entityCull, v -> c.entityCull = v);
            slider(right, y, colW, rowH, "Refresh ticks", 2, 40, c.redstoneFlushTicks, v -> c.redstoneFlushTicks = v);
            y += step;
        } else if (this.page == 3) {
            toggleThen(left, y, colW, rowH, "Hide redstone blk", () -> c.hideRedstoneBlocks,
                v -> c.hideRedstoneBlocks = v, HideManager::reload);
            toggleThen(right, y, colW, rowH, "Solid face fix", () -> c.solidFaceFix,
                v -> c.solidFaceFix = v, HideManager::reload);
            y += step;
            toggle(left, y, colW, rowH, "Hide all drops", () -> c.hideAllItems, v -> c.hideAllItems = v);
            toggle(right, y, colW, rowH, "Hide moving blks", () -> c.hideMovingBlocks, v -> c.hideMovingBlocks = v);
            y += step;
            toggle(left, y, colW, rowH, "Mute redstone all", () -> c.muteAllRedstoneSounds, v -> c.muteAllRedstoneSounds = v);
            toggleThen(right, y, colW, rowH, "Hide fish", () -> c.hideFish, v -> c.hideFish = v, HideManager::rebuild);
            y += step;
            addDrawableChild(ButtonWidget.builder(Text.literal("Hide items..."), b -> {
                if (this.client != null) {
                    this.client.setScreen(new PickerScreen(this, PickerScreen.Kind.ITEMS));
                }
            }).dimensions(left, y, colW, rowH).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Hide mobs..."), b -> {
                if (this.client != null) {
                    this.client.setScreen(new PickerScreen(this, PickerScreen.Kind.MOBS));
                }
            }).dimensions(right, y, colW, rowH).build());
            y += step;
            toggle(left, y, colW, rowH, "Flat light farms", () -> c.flatLightNearFarms, v -> c.flatLightNearFarms = v);
            toggle(right, y, colW, rowH, "Action bar", () -> c.actionBar, v -> c.actionBar = v);
            y += step;
            toggle(left, y, colW, rowH, "Freeze fast fluids", () -> c.freezeFluids, v -> c.freezeFluids = v);
            slider(right, y, colW, rowH, "Anim (normal)", 0, 8, c.textureAnimSkipNormal, v -> c.textureAnimSkipNormal = v);
            y += step;
            addDrawableChild(ButtonWidget.builder(Text.literal("Hide storage..."), b -> {
                if (this.client != null) {
                    this.client.setScreen(new PickerScreen(this, PickerScreen.Kind.STORAGE));
                }
            }).dimensions(left, y, colW, rowH).build());
            toggleThen(right, y, colW, rowH, "Storage hidden", () -> c.hideContainers,
                v -> c.hideContainers = v, HideManager::reload);
            y += step;
        } else {
            slider(left, y, colW, rowH, "Max items", 10, 2000, c.maxItems, v -> c.maxItems = v);
            slider(right, y, colW, rowH, "Max XP orbs", 10, 1000, c.maxXpOrbs, v -> c.maxXpOrbs = v);
            y += step;
            slider(left, y, colW, rowH, "Radius", 8, 128, c.cullRadius, v -> c.cullRadius = v);
            slider(right, y, colW, rowH, "Particles/tick", 20, 2000, c.maxParticlesPerTick, v -> c.maxParticlesPerTick = v);
            y += step;
            slider(left, y, colW, rowH, "Same sound/tick", 1, 10, c.maxSameSoundPerTick, v -> c.maxSameSoundPerTick = v);
            slider(right, y, colW, rowH, "Farm trigger", 5, 200, c.farmSensitivity, v -> c.farmSensitivity = v);
            y += step;
            slider(left, y, colW, rowH, "Far block dist", 8, 96, c.blockEntityDistance, v -> c.blockEntityDistance = v);
            slider(left, y + step, colW, rowH, "Anim skip", 0, 8, c.textureAnimSkip, v -> c.textureAnimSkip = v);
            slider(right, y, colW, rowH, "Far mob dist", 8, 96, c.entityRenderDistance, v -> c.entityRenderDistance = v);
            y += step * 2;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close())
            .dimensions(left, y, colW, rowH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("More settings >"), b -> {
            if (this.client != null) {
                this.client.setScreen(new ConfigScreen(this.parent, (this.page + 1) % PAGES));
            }
        }).dimensions(right, y, colW, rowH).build());
    }

    private void cycle(int x, int y, int w, int h, String name, String[] names,
                       java.util.function.IntSupplier get, IntConsumer set) {
        addDrawableChild(ButtonWidget.builder(Text.literal(name + ": " + names[get.getAsInt()]), b -> {
            set.accept((get.getAsInt() + 1) % names.length);
            b.setMessage(Text.literal(name + ": " + names[get.getAsInt()]));
        }).dimensions(x, y, w, h).build());
    }

    private static Text presetText(FarmConfig c) {
        return Text.literal("Preset: " + FarmConfig.PRESET_NAMES[c.preset]);
    }

    private void toggle(int x, int y, int w, int h, String name, BooleanSupplier get, Consumer<Boolean> set) {
        addDrawableChild(ButtonWidget.builder(toggleText(name, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(toggleText(name, get.getAsBoolean()));
        }).dimensions(x, y, w, h).build());
    }

    private void toggleThen(int x, int y, int w, int h, String name, BooleanSupplier get, Consumer<Boolean> set,
                            Runnable after) {
        addDrawableChild(ButtonWidget.builder(toggleText(name, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(toggleText(name, get.getAsBoolean()));
            after.run();
        }).dimensions(x, y, w, h).build());
    }

    private static Text toggleText(String name, boolean on) {
        return Text.literal(name + ": " + (on ? "ON" : "OFF"));
    }

    private void slider(int x, int y, int w, int h, String label, int min, int max, int current, IntConsumer set) {
        addDrawableChild(new IntSlider(x, y, w, h, label, min, max, current, set));
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void removed() {
        FarmConfig.INSTANCE.clamp();
        FarmFPS.saveConfig();
    }

    private static final class IntSlider extends SliderWidget {
        private final String label;
        private final int min;
        private final int max;
        private final IntConsumer onChange;

        IntSlider(int x, int y, int w, int h, String label, int min, int max, int current, IntConsumer onChange) {
            super(x, y, w, h, Text.empty(), (Math.min(Math.max(current, min), max) - min) / (double) (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        private int currentValue() {
            return this.min + (int) Math.round(this.value * (this.max - this.min));
        }

        @Override
        protected void updateMessage() {
            if (this.label != null) {
                setMessage(Text.literal(this.label + ": " + currentValue()));
            }
        }

        @Override
        protected void applyValue() {
            this.onChange.accept(currentValue());
        }
    }
}
