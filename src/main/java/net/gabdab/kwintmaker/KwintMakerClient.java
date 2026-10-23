package net.gabdab.kwintmaker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

public class KwintMakerClient implements ClientModInitializer {
    public static KeyBinding OPEN;

    // стан
    private static boolean active = false;     // булаву вже взято в цьому падінні
    private static int above = 0;              // скільки тіків умови виконуються
    private static int landTicks = 0;
    private static int prevSlot = -1;          // якщо булава була в хотбарі: який слот був вибраний
    private static int swappedInv = -1;        // якщо брали з інвентаря: звідки
    private static int swappedHotbar = -1;     // і в який слот хотбара поміняли

    @Override
    public void onInitializeClient() {
        KwintMakerConfig.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("kwintmaker", "main"));
        OPEN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kwintmaker.open_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN.wasPressed()) {
                if (mc.currentScreen == null && mc.player != null) mc.setScreen(new KwintMakerScreen());
            }
            tick(mc);
        });
    }

    // ------------------------------------------------------------------

    private static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        KwintMakerConfig c = KwintMakerConfig.I;
        if (p == null || mc.world == null || mc.interactionManager == null) {
            reset();
            return;
        }

        boolean landed = p.isOnGround() || p.isTouchingWater() || p.isInLava() || p.hasVehicle() || p.isClimbing();

        if (active) {
            if (landed) {
                landTicks++;
                if (landTicks >= c.returnDelay) {
                    if (c.returnAfterLand) restore(mc, p);
                    reset();
                }
            }
            return;
        }

        if (!c.enabled || mc.currentScreen != null || p.isSpectator() || landed) {
            above = 0;
            return;
        }
        if (c.onlyFalling && p.getVelocity().y > -0.1) {
            above = 0;
            return;
        }
        if (c.skipGliding && p.isGliding()) {
            above = 0;
            return;
        }

        double h = heightAboveGround(mc, p);
        if (h < c.height) {
            above = 0;
            return;
        }

        above++;
        if (above > c.delay) switchToMace(mc, p);
    }

    /** Відстань від ніг гравця до першого твердого блоку під ним. */
    private static double heightAboveGround(MinecraftClient mc, ClientPlayerEntity p) {
        Vec3d start = new Vec3d(p.getX(), p.getY(), p.getZ());
        Vec3d end = start.add(0, -400, 0);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                p));
        if (hit.getType() == HitResult.Type.MISS) return Double.MAX_VALUE; // під ногами порожнеча
        return start.y - hit.getPos().y;
    }

    private static void switchToMace(MinecraftClient mc, ClientPlayerEntity p) {
        KwintMakerConfig c = KwintMakerConfig.I;
        PlayerInventory inv = p.getInventory();

        if (p.getMainHandStack().isOf(Items.MACE)) {
            markActive(p, "булава вже в руці");
            return;
        }

        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(Items.MACE)) {
                prevSlot = inv.getSelectedSlot();
                inv.setSelectedSlot(i);
                markActive(p, "булава з хотбара");
                return;
            }
        }

        if (c.fromInventory) {
            for (int i = 9; i < 36; i++) {
                if (inv.getStack(i).isOf(Items.MACE)) {
                    int hot = inv.getSelectedSlot();
                    mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, i, hot, SlotActionType.SWAP, p);
                    swappedInv = i;
                    swappedHotbar = hot;
                    markActive(p, "булава з інвентаря");
                    return;
                }
            }
        }
        // булави немає - не пробуємо знову в цьому падінні
        active = true;
        landTicks = 0;
    }

    private static void markActive(ClientPlayerEntity p, String msg) {
        active = true;
        landTicks = 0;
        if (KwintMakerConfig.I.showMessages) p.sendMessage(Text.literal("KwintMaker: " + msg), true);
    }

    private static void restore(MinecraftClient mc, ClientPlayerEntity p) {
        PlayerInventory inv = p.getInventory();
        if (swappedInv >= 0 && swappedHotbar >= 0 && inv.getStack(swappedHotbar).isOf(Items.MACE)) {
            mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, swappedInv, swappedHotbar, SlotActionType.SWAP, p);
        }
        if (prevSlot >= 0 && prevSlot < 9) inv.setSelectedSlot(prevSlot);
    }

    private static void reset() {
        active = false;
        above = 0;
        landTicks = 0;
        prevSlot = -1;
        swappedInv = -1;
        swappedHotbar = -1;
    }
}
