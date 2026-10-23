package net.gabdab.kwintmaker;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class KwintMakerScreen extends Screen {
    private static final int W = 320, H = 176;

    private boolean listening = false;
    private ButtonWidget bindBtn;
    private int left, top, colW;

    public KwintMakerScreen() {
        super(Text.literal("KwintMaker"));
    }

    private int cx(int col) {
        return left + 10 + col * (colW + 10);
    }

    private int ry(int row) {
        return top + 26 + row * 23;
    }

    private static KwintMakerConfig c() {
        return KwintMakerConfig.I;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        colW = (W - 30) / 2;

        toggle(cx(0), ry(0), W - 20, "Мод", () -> c().enabled, v -> c().enabled = v);
        addDrawableChild(new ConfigSlider(cx(0), ry(1), W - 20, 20, "Висота над землею (блоки)", 3, 60, 0.5,
                () -> c().height, v -> c().height = v));
        toggle(cx(0), ry(2), colW, "Тільки при падінні", () -> c().onlyFalling, v -> c().onlyFalling = v);
        toggle(cx(1), ry(2), colW, "Брати з інвентаря", () -> c().fromInventory, v -> c().fromInventory = v);
        toggle(cx(0), ry(3), colW, "Повертати після", () -> c().returnAfterLand, v -> c().returnAfterLand = v);
        addDrawableChild(new ConfigSlider(cx(1), ry(3), colW, 20, "Затримка повернення", 0, 20, 1,
                () -> c().returnDelay, v -> c().returnDelay = (int) Math.round(v)));
        toggle(cx(0), ry(4), colW, "Не на елітрах", () -> c().skipGliding, v -> c().skipGliding = v);
        addDrawableChild(new ConfigSlider(cx(1), ry(4), colW, 20, "Затримка вибору", 0, 10, 1,
                () -> c().delay, v -> c().delay = (int) Math.round(v)));

        bindBtn = ButtonWidget.builder(bindText(), b -> {
            listening = true;
            b.setMessage(Text.literal("> натисни клавішу <").formatted(Formatting.YELLOW));
        }).dimensions(cx(0), ry(5), colW, 20).build();
        addDrawableChild(bindBtn);
        toggle(cx(1), ry(5), colW, "Повідомлення", () -> c().showMessages, v -> c().showMessages = v);
    }

    private void toggle(int x, int y, int w, String label, BooleanSupplier g, Consumer<Boolean> s) {
        addDrawableChild(ButtonWidget.builder(toggleText(label, g.getAsBoolean()), b -> {
            s.accept(!g.getAsBoolean());
            b.setMessage(toggleText(label, g.getAsBoolean()));
            KwintMakerConfig.save();
        }).dimensions(x, y, w, 20).build());
    }

    private static Text toggleText(String label, boolean on) {
        return Text.literal(label + ": ").append(Text.literal(on ? "УВІМК" : "ВИМК").formatted(on ? Formatting.GREEN : Formatting.RED));
    }

    private static Text bindText() {
        return Text.literal("Бінд: ").append(KwintMakerClient.OPEN.getBoundKeyLocalizedText());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (listening) {
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                KwintMakerClient.OPEN.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(key));
                KeyBinding.updateKeysByCode();
                if (client != null) client.options.write();
            }
            listening = false;
            if (bindBtn != null) bindBtn.setMessage(bindText());
            return true;
        }
        if (key == KeyBindingHelper.getBoundKeyOf(KwintMakerClient.OPEN).getCode()) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        KwintMakerConfig.save();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(left, top, left + W, top + H, 0xD0101018);
        int border = 0xFF4C9BFF;
        ctx.fill(left, top, left + W, top + 1, border);
        ctx.fill(left, top + H - 1, left + W, top + H, border);
        ctx.fill(left, top, left + 1, top + H, border);
        ctx.fill(left + W - 1, top, left + W, top + H, border);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("KwintMaker"), width / 2, top + 8, 0xFF8AC8FF);
    }
}
