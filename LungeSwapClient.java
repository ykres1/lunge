package com.lungeswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class LungeSwapClient implements ClientModInitializer {
    private static KeyBinding comboKey;
    private static int delayTicks = 3;

    // 0 = idle, 1 = wind charge thrown (waiting), 2 = spear selected (waiting), 3 = jab release
    private static int stage = 0;
    private static int wait = 0;
    private static int returnSlot = -1;

    @Override
    public void onInitializeClient() {
        loadConfig();
        comboKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lungeswap.combo",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyBinding.Category.create(Identifier.of("lungeswap", "main"))));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null) {
            stage = 0;
            return;
        }
        PlayerInventory inv = mc.player.getInventory();

        if (stage == 0) {
            while (comboKey.wasPressed()) {
                int charge = findHotbar(inv, false);
                int spear = findHotbar(inv, true);
                if (charge < 0 || spear < 0) {
                    mc.player.sendMessage(net.minecraft.text.Text.literal(
                            "Lunge Swap: need a wind charge and a Lunge spear in your hotbar"), true);
                    continue;
                }
                returnSlot = inv.getSelectedSlot();
                inv.setSelectedSlot(charge);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                stage = 1;
                wait = delayTicks;
                return;
            }
            return;
        }

        if (wait > 0) {
            wait--;
            return;
        }

        if (stage == 1) {
            int spear = findHotbar(inv, true);
            if (spear >= 0) inv.setSelectedSlot(spear);
            stage = 2;
            wait = delayTicks;
        } else if (stage == 2) {
            // jab with the spear (left click)
            KeyBinding.onKeyPressed(mc.options.attackKey.getDefaultKey());
            stage = 3;
            wait = 1;
        } else if (stage == 3) {
            stage = 0;
        }
    }

    private static int findHotbar(PlayerInventory inv, boolean spearWithLunge) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            if (!spearWithLunge) {
                if (s.isOf(Items.WIND_CHARGE)) return i;
            } else {
                String path = Registries.ITEM.getId(s.getItem()).getPath();
                if (path.endsWith("_spear") && hasLunge(s)) return i;
            }
        }
        return -1;
    }

    private static boolean hasLunge(ItemStack stack) {
        for (var entry : stack.getEnchantments().getEnchantments()) {
            if (entry.getKey().map(k -> k.getValue().getPath().equals("lunge")).orElse(false)) return true;
        }
        return false;
    }

    private static void loadConfig() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("lungeswap.properties");
        Properties props = new Properties();
        try {
            if (Files.exists(p)) {
                props.load(Files.newBufferedReader(p));
                delayTicks = Math.max(0, Integer.parseInt(props.getProperty("delay_ticks", "3").trim()));
            } else {
                props.setProperty("delay_ticks", "3");
                props.store(Files.newBufferedWriter(p), "Ticks between each step of the combo (default 3)");
            }
        } catch (IOException | NumberFormatException ignored) {
        }
    }
}
