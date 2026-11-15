package com.farmfps.client.gui;

import com.farmfps.FarmFPS;
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
 * Two-page settings screen with large toggles and sliders, no hover or keyboard needed, so it
 * works with touch controls. Opens from Mod Menu (if installed) or with /farmfps.
 */
public class ConfigScreen extends Screen {
    private static final int PAGES = 2;

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
        int rows = this.page == 0 ? 5 : 7;
        int rowH = Math.max(20, Math.min(26, (this.height - 40) / rows - 4));
        int step = rowH + 4;
        int y = 28;

        Text heading = Text.literal("FarmFPS Settings  (" + (this.page + 1) + "/" + PAGES + ")");
        int titleW = this.textRenderer.getWidth(heading);
        addDrawableChild(new TextWidget((this.width - titleW) / 2, 10, titleW, 9, heading, this.textRenderer));

        if (this.page == 0) {
            toggle(left, y, colW, rowH, "Mod", () -> c.enabled, v -> c.enabled = v);
            toggle(right, y, colW, rowH, "Piston snap", () -> c.pistonSnap, v -> c.pistonSnap = v);
            y += step;
            toggle(left, y, colW, rowH, "Cap items", () -> c.capItems, v -> c.capItems = v);
            toggle(right, y, colW, rowH, "Cap XP orbs", () -> c.capXpOrbs, v -> c.capXpOrbs = v);
            y += step;
            toggle(left, y, colW, rowH, "Drop extra items", () -> c.aggressiveItems, v -> c.aggressiveItems = v);
            toggle(right, y, colW, rowH, "Hide farm fx", () -> c.hideFarmEffects, v -> c.hideFarmEffects = v);
            y += step;
            toggle(left, y, colW, rowH, "Fx budgets", () -> c.throttleParticles && c.throttleSounds,
                v -> { c.throttleParticles = v; c.throttleSounds = v; });
            addDrawableChild(ButtonWidget.builder(presetText(c), b -> {
                c.applyPreset((c.preset + 1) % FarmConfig.PRESET_NAMES.length);
                b.setMessage(presetText(c));
            }).dimensions(right, y, colW, rowH).build());
            y += step;
        } else {
            toggle(left, y, colW, rowH, "Redstone lock", () -> c.redstoneLock, v -> c.redstoneLock = v);
            toggle(right, y, colW, rowH, "Redstone sounds", () -> c.muteRedstoneSounds, v -> c.muteRedstoneSounds = v);
            y += step;
            toggle(left, y, colW, rowH, "Cull far blocks", () -> c.blockEntityCull, v -> c.blockEntityCull = v);
            toggle(right, y, colW, rowH, "Action bar", () -> c.actionBar, v -> c.actionBar = v);
            y += step;
            slider(left, y, colW, rowH, "Max items", 10, 2000, c.maxItems, v -> c.maxItems = v);
            slider(right, y, colW, rowH, "Max XP orbs", 10, 1000, c.maxXpOrbs, v -> c.maxXpOrbs = v);
            y += step;
            slider(left, y, colW, rowH, "Radius", 8, 128, c.cullRadius, v -> c.cullRadius = v);
            slider(right, y, colW, rowH, "Particles/tick", 20, 2000, c.maxParticlesPerTick, v -> c.maxParticlesPerTick = v);
            y += step;
            slider(left, y, colW, rowH, "Same sound/tick", 1, 10, c.maxSameSoundPerTick, v -> c.maxSameSoundPerTick = v);
            slider(right, y, colW, rowH, "Farm trigger", 5, 200, c.farmSensitivity, v -> c.farmSensitivity = v);
            y += step;
            slider(left, y, colW, rowH, "Redstone ticks", 2, 20, c.redstoneFlushTicks, v -> c.redstoneFlushTicks = v);
            slider(right, y, colW, rowH, "Far block dist", 8, 96, c.blockEntityDistance, v -> c.blockEntityDistance = v);
            y += step;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close())
            .dimensions(left, y, colW, rowH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("More settings >"), b -> {
            if (this.client != null) {
                this.client.setScreen(new ConfigScreen(this.parent, (this.page + 1) % PAGES));
            }
        }).dimensions(right, y, colW, rowH).build());
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
