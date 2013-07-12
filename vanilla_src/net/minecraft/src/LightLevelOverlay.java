package net.minecraft.src;

import java.io.File;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GLContext;

/**
 * LightLevelOverlay Minecraft mod.
 * 
 * This mod renders the light level on top of blocks around the player,
 * this helps to identify areas where mobs can spawn.
 * 
 * In vanilla this class is invoked in RenderGlobal at the end of drawBlockDamageTexture(),
 * in forge the event hook RenderWorldLastEvent can be used.
 * 
 * place at end of RenderGlobal.drawBlockDamageTexture (func_72717_a in older mcp versions):
 *
 *   LightLevelOverlay.getInstance().render(globalRenderBlocks, par3);
 *
 * This takes care of everything. 
 *
 * @author apoc <http://apoc.cc>
 * @license 3-clause BSD
 */
class LightLevelOverlay {
    private static LightLevelOverlay instance = null;
    
    public static LightLevelOverlay getInstance() {
        if (instance == null) {
            instance = new LightLevelOverlay();
        }
        return instance;
    }

    private LightLevelOverlayRenderer renderer;
    private LightLevelOverlayThread thread;
    private LightLevelOverlayConfig config;

    // manual keypress event handling for the hotkey
    private long lastHotkeyKeydown;
    // whenever the overlay should be rendered
    private boolean active = false;
    // point in time of the frame
    private long frameTime;

    
    private Minecraft mc;
    

    private LightLevelOverlay() {
        mc = Minecraft.getMinecraft();

        File file = new File(Minecraft.getMinecraft().mcDataDir, "config/lloverlay.properties");
        config = new LightLevelOverlayConfig(file);
        
        debugMessage("loading");
        reload();
    }


    public void render(RenderBlocks renderBlocks, float partialTickTime) {
        thread.setRenderBlocks(renderBlocks);
        frameTime = System.currentTimeMillis();
        if (mc.currentScreen == null)
            hotkeyPoll();
        if(!active) return;
        
        EntityPlayer player = mc.thePlayer;
        
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTickTime;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTickTime;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTickTime;

        renderer.render(x, y, z);
    }
    
    private void reload() {
        if ((config.getRenderer() == LightLevelOverlayConfig.Renderer.FAST ||
            config.getRenderer() == LightLevelOverlayConfig.Renderer.AUTO) &&
           GLContext.getCapabilities().OpenGL15) {
           renderer = new LightLevelOverlayRendererVBO(config);
           debugMessage("using fast VBO renderer");
        }
        else {
            renderer = new LightLevelOverlayRendererVanilla(config);
            debugMessage("using slow vanilla legacy renderer");
        }
        thread = new LightLevelOverlayThread(config, renderer);
    }

    private void hotkeyPoll() {
        if (Keyboard.isKeyDown(config.getHotkey()) && frameTime - lastHotkeyKeydown > 250) {
            lastHotkeyKeydown = frameTime;
            if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
                config.load();
                debugMessage("reload config");
                thread.setActive(false);
                thread.interrupt();
                reload();
            }
            else {
                active = (active) ? false : true; // toggle!
                thread.setActive(active);
                debugMessage("toggle active: %s", active);
                renderer.clear();
                if (!thread.isAlive()) {
                    debugMessage("starting thread");
                    try {
                        thread.start();
                    }
                    catch (Exception e) {
                        debugMessage("unable to start lloverlay thread!");
                        e.printStackTrace();
                        thread = new LightLevelOverlayThread(config, renderer);
                    }
                }
            }
        }
    }

    private void debugMessage(String fmt, Object... args) {
        if (config.isDebug()) {
            String message = String.format(fmt, args);
            System.err.printf("[LightLevelOverlay] %s\n", message);
        }
    }
    
}
