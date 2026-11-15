package CHANGE.ME.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;

/**
 * Temporary visual check (Yarn 1.21.1 names). Enabled with -D&lt;mod&gt;.&lt;flag&gt;=true; see the skill
 * fabric-visual-test. Creates a flat creative world, runs {@link #script} once per tick and quits.
 */
final class VisualCheck {

    /** Screenshot file prefix: run/screenshots/PREFIX-<tick>.png */
    private static final String PREFIX = "check";
    /** Ticks after joining when screenshots are taken. */
    private static final int[] SHOTS = {60, 80, 110, 142, 150, 160, 180, 220};
    /** Tick after joining when the game quits. */
    private static final int END = 240;

    private static boolean created;
    private static int ticks;

    private VisualCheck() {
    }

    static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!created) {
                if (!(client.currentScreen instanceof TitleScreen) || client.getOverlay() != null) return;
                created = true;
                client.options.pauseOnLostFocus = false;
                client.options.getGuiScale().setValue(2);
                client.createIntegratedServerLoader().createAndStart(PREFIX + "-" + System.currentTimeMillis(),
                        new LevelInfo("Visual check", GameMode.CREATIVE, false, Difficulty.PEACEFUL,
                                true, new GameRules(), DataConfiguration.SAFE_MODE),
                        new GeneratorOptions(0, false, false),
                        registries -> registries.getWrapperOrThrow(RegistryKeys.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)
                                .value().createDimensionsRegistryHolder(), client.currentScreen);
                return;
            }
            // Count only while actually in the world (no menu, no loading overlay).
            if (client.player == null || client.getServer() == null || client.currentScreen != null || client.getOverlay() != null) return;

            int t = ticks++;
            // Same camera every tick so screenshots are comparable.
            client.options.setPerspective(Perspective.FIRST_PERSON);
            client.options.getFov().setValue(70);
            client.player.setYaw(0);
            client.player.setPitch(0);

            script(client, t);

            for (int shot : SHOTS) {
                if (t == shot) {
                    ScreenshotRecorder.saveScreenshot(client.runDirectory, PREFIX + "-" + t + ".png",
                            client.getFramebuffer(), message -> {});
                }
            }
            if (t == END) client.scheduleStop();
        });
    }

    /** The test itself: change this. {@code t} is ticks since joining (20 per second). */
    private static void script(MinecraftClient client, int t) {
        if (t == 0) {
            command(client, "clear @a");
            command(client, "time set noon");
            command(client, "weather clear");
            // command(client, "mymod give my_item @a");
        }
        if (t == 40) client.inGameHud.getChatHud().clear(false);

        // Example: sprint from tick 70 to 140.
        if (t == 70) {
            client.options.forwardKey.setPressed(true);
            client.options.sprintKey.setPressed(true);
        }
        if (t == 140) {
            client.options.forwardKey.setPressed(false);
            client.options.sprintKey.setPressed(false);
        }
        // Hold use (aim) / attack:  client.options.useKey.setPressed(true);  client.options.attackKey.setPressed(true);
        // Single key press (e.g. a mod key bound to R):  tap(org.lwjgl.glfw.GLFW.GLFW_KEY_R);
    }

    /** Runs a command on the integrated server as the server (permission level 4). */
    private static void command(MinecraftClient client, String command) {
        var server = client.getServer();
        server.execute(() -> server.getCommandManager().executeWithPrefix(server.getCommandSource(), command));
    }

    /** One press of the key bound to {@code glfwKey} (what wasPressed() sees). */
    @SuppressWarnings("unused")
    private static void tap(int glfwKey) {
        KeyBinding.onKeyPressed(InputUtil.fromKeyCode(glfwKey, 0));
    }
}
