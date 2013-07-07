package net.minecraft.src;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.lwjgl.input.Keyboard;

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

    // debug messages are printed to stderr
    private boolean debug = false;
    // manual keypress event handling for the hotkey
    private long lastHotkeyKeydown;
    // toggles the overlay on/off
    private int hotkey = Keyboard.KEY_F9;
    // whenever the overlay should be rendered
    private boolean active = false;
    // point in time of the frame
    private long frameTime;
    // the lightlevel.png texture file contains 
    private int textureRow = 0;
    // only draw overlay for lightlevel <n> and lower.
    private int showLightlevelUpto = 15;
    // interval in ms in which the overlay cache should be generated,
    // for instance if you place a torch its at least 250 ms till the
    // overlays are updated
    private int generateInterval = 250;
    // overlay drawing area around the player in blocks in each direction
    private int drawDistance = 25; // actual area: drawDistance^3*2
    
    // show the lightlevel affected by the sun
    private boolean useSkyLightlevel = false;

    // if set draws the overlay for blocks that ordinarily
    // don't allow mob spawning.
    private boolean drawNonSpawnable = false;
    
    private static int[] NON_SPAWNABLE_EXCEPTIONS = new int[] { 
        Block.tilledField.blockID,
        Block.woodSingleSlab.blockID, 
        Block.stoneSingleSlab.blockID,
        Block.glass.blockID, 
        Block.snow.blockID, 
        Block.ice.blockID,
        Block.glowStone.blockID, 
        Block.pistonBase.blockID,
        Block.pistonStickyBase.blockID,
        Block.pressurePlateStone.blockID,
        Block.pressurePlatePlanks.blockID,
        Block.pressurePlateGold.blockID,
        Block.pressurePlateIron.blockID,
        Block.daylightSensor.blockID,
        Block.field_111031_cC.blockID // carpet
    };
    
    private Minecraft mc;

    private File configFile;
    private Properties configProperties;
    
    private LightLevelOverlayRenderer renderer;
    
    private GenerateThread thread;
    
    private RenderBlocks renderBlocks;

    private LightLevelOverlay() {
        debugMessage("loading");

        mc = Minecraft.getMinecraft();

        configFile = new File(mc.mcDataDir, "config/lloverlay.properties");
        if (!configFile.getParentFile().exists()) {
            // create config directory
            configFile.getParentFile().mkdir();
        }
        if (!configFile.exists()) {
            saveConfig(); // saves default config
        }
        loadConfig();
        
        renderer = new LightLevelOverlayRenderer();
        thread = new GenerateThread();
    }

    private void saveConfig() {
        configProperties = new Properties();
        configProperties.setProperty("drawDistance", Integer.toString(drawDistance));
        configProperties.setProperty("hotkey", Integer.toString(hotkey));
        configProperties.setProperty("generateInterval", Integer.toString(generateInterval));
        configProperties.setProperty("textureRow", Integer.toString(textureRow));
        configProperties.setProperty("debug", Boolean.toString(debug));
        configProperties.setProperty("drawNonSpawnable", Boolean.toString(drawNonSpawnable));
        configProperties.setProperty("showLightlevelUpto", Integer.toString(showLightlevelUpto));
        configProperties.setProperty("useSkyLightlevel", Boolean.toString(useSkyLightlevel));
        try {
            configProperties.store(new FileOutputStream(configFile), "Lightlevel Overlay Config");
            debugMessage("config saved: %s", configFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        configProperties = new Properties();
        try {
            configProperties.load(new FileInputStream(configFile));
            drawDistance = Integer.parseInt(configProperties.getProperty("drawDistance"));
            hotkey = Integer.parseInt(configProperties.getProperty("hotkey"));
            generateInterval = Integer.parseInt(configProperties.getProperty("generateInterval"));
            textureRow = Integer.parseInt(configProperties.getProperty("textureRow"));
            debug = Boolean.parseBoolean(configProperties.getProperty("debug"));
            // for backwards-compat.:
            if (configProperties.containsKey("drawNonSpawnable")) {
                drawNonSpawnable = Boolean.parseBoolean(configProperties.getProperty("drawNonSpawnable"));
            }
            else {
                saveConfig();
            }
            if (configProperties.containsKey("showLightlevelUpto")) {
                showLightlevelUpto = Integer.parseInt(configProperties.getProperty("showLightlevelUpto"));
            }
            else {
                saveConfig();
            }
            if (configProperties.containsKey("useSkyLightlevel")) {
                useSkyLightlevel = Boolean.parseBoolean(configProperties.getProperty("useSkyLightlevel"));
            }
            else {
                saveConfig();
            }
            debugMessage("config loaded: %s", configFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }   
    }


    public void render(RenderBlocks renderBlocks, float partialTickTime) {
        this.renderBlocks = renderBlocks;
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

    private void hotkeyPoll() {
        if (Keyboard.isKeyDown(hotkey) && frameTime - lastHotkeyKeydown > 250) {
            lastHotkeyKeydown = frameTime;
            if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
                loadConfig();
                debugMessage("reload config");
                renderer.clear();
            }
            else {
                active = (active) ? false : true; // toggle!
                debugMessage("toggle active: %s", active);
                renderer.clear();
                if (!thread.isAlive()) {
                    debugMessage("starting thread");
                    thread.start();
                }
            }
        }
    }

    private void debugMessage(String fmt, Object... args) {
        if (debug) {
            String message = String.format(fmt, args);
            System.err.printf("[%s] - LightlevelOverlay - %s\n", (new Date()).toString(), message);
        }
    }
    
    private boolean isNonSpawnable(Block block) {
        if (!drawNonSpawnable) return false;
        for (int id : NON_SPAWNABLE_EXCEPTIONS)
            if (block.blockID == id)
                return true;
        return false;
    }

    private void generate() throws Exception {
        if (renderBlocks == null) return;
        renderer.startGenerate();

        double yOffset;
        
        long tStart = System.currentTimeMillis();
        
        //IChunkProvider provider = mc.theWorld.getChunkProvider();
        //int chunks = 4;
        //for (int chunkX = mc.thePlayer.chunkCoordX - chunks; chunkX < mc.thePlayer.chunkCoordX + chunks; chunkX++) {
        //    for (int chunkZ = mc.thePlayer.chunkCoordZ - chunks; chunkZ < mc.thePlayer.chunkCoordZ + chunks; chunkZ++) {
        //        if (provider.chunkExists(chunkX, chunkZ)) {
        //        }
        //    }
        //}

        int posX = (int) Math.floor(mc.thePlayer.posX);
        int posY = (int) Math.floor(mc.thePlayer.posY);
        int posZ = (int) Math.floor(mc.thePlayer.posZ);

        for (int x = posX-drawDistance; x < posX+drawDistance; x++) {
            for (int z = posZ-drawDistance; z < posZ+drawDistance; z++) {
                for (int y = posY+drawDistance; y > posY-drawDistance; y--) {
                    yOffset = 0.0;
                    
                    // get the blocktype of that location in the world
                    Block block = Block.blocksList[mc.theWorld.getBlockId(x, y, z)];
                    
                    // should be airblock or not opaque
                    if (block != null && !block.isOpaqueCube())
                        continue;

                    // underneath must not be airblock
                    Block blockUnder = Block.blocksList[mc.theWorld.getBlockId(x, y - 1, z)];
                    if (blockUnder == null)
                        continue;
                     
                    // only consider if solid surface
                    if (!mc.theWorld.doesBlockHaveSolidTopSurface(x, y - 1, z)) {
                        if (isNonSpawnable(blockUnder)) {
                            blockUnder.setBlockBoundsBasedOnState(renderBlocks.blockAccess, x,y-1,z);
                            yOffset = blockUnder.getBlockBoundsMaxY() - 1.0;
                        }
                        else {
                            continue;
                        }
                    }
                    
                    int texture;
                    if (useSkyLightlevel) {
                        texture = mc.theWorld.getSavedLightValue(EnumSkyBlock.Sky, x, y + 1, z); 
                    }
                    else {
                        texture = mc.theWorld.getSavedLightValue(EnumSkyBlock.Block, x, y + 1, z);
                    }
                    
                    if (texture > showLightlevelUpto) {
                        continue;
                    }
                    texture += (textureRow * 16);

                    renderer.addOverlay(x, y, z, yOffset, texture);
                    y--; // no reason to check block under again
                }
            }
        }
        
        debugMessage("generation took %dms\n", System.currentTimeMillis() - tStart);
        
        renderer.stopGenerate();
    }
    
    
    private class GenerateThread extends Thread {
        public GenerateThread() {
            setName("lloverlay");
        }
        
        public void run() {
            try {
                while (true) {
                    if (active) {
                        generate();
                    }
                    
                    sleep(generateInterval);
                }
            }
            catch (Exception e) {
            }
            
            debugMessage("overlay thread stopped");
        }
    }
}
