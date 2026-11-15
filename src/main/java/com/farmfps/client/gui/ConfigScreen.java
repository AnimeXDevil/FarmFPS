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
 * Settings screen: two columns of large toggles and sliders, no hover or keyboard needed,
 * so it works with touch controls. Opens from Mod Menu (if installed) or with /farmfps.
 */
public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Text.literal("FarmFPS Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        FarmConfig c = FarmConfig.INSTANCE;

        int gap = 6;
        int colW = Math.max(100, Math.min(160, (this.width - 24 - gap) / 2));
        int left = (this.width - (colW * 2 + gap)) / 2;
        int right = left + colW + gap;
        int rows = 7; // 6 settings rows + Done
        int rowH = Math.max(20, Math.min(26, (this.height - 40) / rows - 4));
        int step = rowH + 4;
        int y = 28;

        int titleW = this.textRenderer.getWidth(this.title);
        addDrawableChild(new TextWidget((this.width - titleW) / 2, 10, titleW, 9, this.title, this.textRenderer));

        toggle(left, y, colW, rowH, "Mod enabled", () -> c.enabled, v -> c.enabled = v);
        toggle(right, y, colW, rowH, "Piston snap", () -> c.pistonSnap, v -> c.pistonSnap = v);
        y += step;
        toggle(left, y, colW, rowH, "Cap items", () -> c.capItems, v -> c.capItems = v);
        toggle(right, y, colW, rowH, "Cap XP orbs", () -> c.capXpOrbs, v -> c.capXpOrbs = v);
        y += step;
        toggle(left, y, colW, rowH, "Particles", () -> c.throttleParticles, v -> c.throttleParticles = v);
        toggle(right, y, colW, rowH, "Sounds", () -> c.throttleSounds, v -> c.throttleSounds = v);
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

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close())
            .dimensions(left, y, colW * 2 + gap, rowH).build());
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
