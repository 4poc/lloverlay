package net.minecraft.src;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

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
    
    
    
    /**
     * List of non-opaque blocks we draw overlays onto.
     */
    private static int[] OVERLAY_BLOCKS = new int[] {
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
        Block.leaves.blockID,
        Block.field_111031_cC.blockID // carpet
    };
    
    private Minecraft mc;

    private File configFile;
    private Properties configProperties;
    
    private LightLevelOverlayRenderer renderer;
    
    private GenerateThread thread;
    
    private RenderBlocks renderBlocks;
    
    
    private int playerX;
    private int playerY;
    private int playerZ;
    private int fillDistance = 50;
    
    

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
                    try {
                        thread.start();
                    }
                    catch (Exception e) {
                        debugMessage("unable to start lloverlay thread!");
                        e.printStackTrace();
                    }
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
    
    private boolean isOverlayBlock(Block block) {
        for (int id : OVERLAY_BLOCKS)
            if (block.blockID == id)
                return true;
        return false;
    }
    
    private class Position {
        public int x;
        public int y;
        public int z;
        private Block block;
        
        public Position(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public Block getBlock() {
            if (block == null)
                block = Block.blocksList[mc.theWorld.getBlockId(x, y, z)];
            return block;
        }

        public boolean isEmpty() {
            return getBlock() == null || !getBlock().isOpaqueCube();
        }
        
        public boolean isOverlayBlock() {
            return getBlock() != null && LightLevelOverlay.this.isOverlayBlock(getBlock());
        }

        public boolean isOpaqueCube() {
            return getBlock() != null && getBlock().isOpaqueCube();
        }
        
        public Position getTop() {
            return new Position(x, y + 1, z);
        }
        public Position getBottom() {
            return new Position(x, y - 1, z);
        }
        public Position getLeft() {
            return new Position(x - 1, y, z);
        }
        public Position getRight() {
            return new Position(x + 1, y, z);
        }
        public Position getFront() {
            return new Position(x, y, z + 1);
        }
        public Position getBack() {
            return new Position(x, y, z - 1);
        }

        // source: Effective Java
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Position that = (Position) o;

            if (x != that.x) return false;
            if (y != that.y) return false;
            if (z != that.z) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (x ^ (x >>> 16));
            result = 31 * result + (y ^ (y >>> 16));
            result = 31 * result + (z ^ (z >>> 16));

            return result;
        }

        public String toString() {
            return String.format("x=%d, y=%d, z=%d", x, y, z);
        }
    }
    
    private Set<Position> visited = new HashSet<Position>();
    
    private Deque<Position> queue = new LinkedList<Position>(); // LIFO last in first out
    
    private void addToQueue(Position pos) {
        if (pos != null && !visited.contains(pos) && (!pos.isOpaqueCube() && !pos.isOverlayBlock())) {
            queue.addLast(pos);
        }
    }

    /**
     * Collect overlay information via a flood filling algorithm.
     * 
     * The amount of overlays to render dramatically decreases (in some cases), however
     * it takes much longer to generate. Unused atm.
     * 
     * @throws Exception
     */
    private void generate() throws Exception {
        if (renderBlocks == null) return;
        // indicates the renderer to get a new set of overlays
        renderer.startGenerate();

        long tStart = System.currentTimeMillis();
        
        int posX = (int) Math.floor(mc.thePlayer.posX);
        int posY = (int) Math.floor(mc.thePlayer.posY);
        int posZ = (int) Math.floor(mc.thePlayer.posZ);
        
        playerX = (int) Math.ceil(mc.thePlayer.posX);
        playerY = (int) Math.ceil(mc.thePlayer.posY);
        playerZ = (int) Math.ceil(mc.thePlayer.posZ);
        
        visited.clear();
        queue.clear();

        int distance = drawDistance;
        
        debugMessage("start generation by flood fill");
        
        // start at the players location:
        queue.addLast(new Position(posX, posY, posZ));
        while (queue.size() > 0) {
            Position pos = queue.removeFirst();
            if (!visited.contains(pos)) {
                visited.add(pos);

                if ((Math.abs(pos.x - posX) < distance) &&
                    (Math.abs(pos.y - posY) < distance) &&
                    (Math.abs(pos.z - posZ) < distance)) {

                    // there 6 neighboring blocks to check
                    if (pos.y < posY+3) { // begin iteration beneath the players head (small optimization)
                        addToQueue(pos.getTop());
                    }
                    
                    // current block is air (we already know that),
                    // the bottom block however might not be:
                    Position overlayPos = pos.getBottom();
                    if (!visited.contains(overlayPos) && (overlayPos.isOpaqueCube() || overlayPos.isOverlayBlock())) {
                        Block block = overlayPos.getBlock();
                        // the height of the block
                        double blockHeight = 1.0;
                        if (!mc.theWorld.doesBlockHaveSolidTopSurface(overlayPos.x, overlayPos.y, overlayPos.z)) {
                            block.setBlockBoundsBasedOnState(renderBlocks.blockAccess, overlayPos.x, overlayPos.y, overlayPos.z);
                            blockHeight = block.getBlockBoundsMaxY();
                        }
                        
                        // the light level of the block above it
                        int texture;
                        if (useSkyLightlevel) {
                            texture = mc.theWorld.getSavedLightValue(EnumSkyBlock.Sky, overlayPos.x, overlayPos.y + 1, overlayPos.z); 
                        }
                        else {
                            texture = mc.theWorld.getSavedLightValue(EnumSkyBlock.Block, overlayPos.x, overlayPos.y + 1, overlayPos.z);
                        }
                        
                        if (texture <= showLightlevelUpto) {
                            texture += (textureRow * 16);
                            renderer.addOverlay(overlayPos.x, overlayPos.y, overlayPos.z, blockHeight, texture);
                        }
                    }
                    addToQueue(overlayPos);

                    addToQueue(pos.getLeft());
                    addToQueue(pos.getRight());
                    addToQueue(pos.getFront());
                    addToQueue(pos.getBack());
                }
            }
        }

        debugMessage("generation took %dms for %d overlays", System.currentTimeMillis() - tStart, renderer.getCacheSize());
        
        renderer.stopGenerate();
    }
    
    

    private void generateByChunk() throws Exception {
        if (renderBlocks == null) return;
        // indicates the renderer to get a new set of overlays
        renderer.startGenerate();

        long tStart = System.currentTimeMillis();
        
        int posX = (int) Math.floor(mc.thePlayer.posX);
        int posY = (int) Math.floor(mc.thePlayer.posY);
        int posZ = (int) Math.floor(mc.thePlayer.posZ);
        
        debugMessage("start generation by chunk");

        int chunkDistance = (int) Math.ceil(drawDistance / 16.0);
        
        // collect block & lighting information per chunks around the player
        IChunkProvider provider = mc.theWorld.getChunkProvider();
        for (int chunkX = mc.thePlayer.chunkCoordX - chunkDistance; chunkX <= mc.thePlayer.chunkCoordX + chunkDistance; chunkX++) {
            for (int chunkZ = mc.thePlayer.chunkCoordZ - chunkDistance; chunkZ <= mc.thePlayer.chunkCoordZ + chunkDistance; chunkZ++) {
                if (provider.chunkExists(chunkX, chunkZ)) {
                    Chunk chunk = provider.provideChunk(chunkX, chunkZ);
                    
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            Block previous = null;
                            // begin iteration above the players head (small optimization)
                            for (int y = posY+3; y > posY+3-drawDistance; y--) {
                                
                                // local chunk coords => world coords
                                int wx = chunkX * 16 + x;
                                int wz = chunkZ * 16 + z;
                                
                                Block block = Block.blocksList[chunk.getBlockID(wx & 15, y, wz & 15)];
                                
                                // ignore air blocks
                                if (block != null) {
                                    // check if it is a block where we draw onto (stone,grass,pistons,pressure plates)
                                    if (block.isOpaqueCube() || isOverlayBlock(block)) {
                                        if (previous == null) {
                                            // the height of the block
                                            double blockHeight = 1.0;
                                            //if ()

                                            
                                            boolean solidTop = mc.theWorld.doesBlockHaveSolidTopSurface(wx, y, wz);
                                            if (!solidTop) {
                                                //System.out.println("asdf");
                                                block.setBlockBoundsBasedOnState(renderBlocks.blockAccess, wx, y, wz);
                                                blockHeight = block.getBlockBoundsMaxY();

                                            }
                                            

                                            
                                            // the light level of the block above it
                                            
                                            int texture;
                                            int blockLightLevel;
                                            // thats for snow/pressure plates vs. upsidedown-halfslabs
                                            if (isOverlayBlock(block)) {
                                                if (blockHeight >= .5) {
                                                    blockLightLevel = y + 1;
                                                }
                                                else {
                                                    blockLightLevel = y;
                                                }
                                            }
                                            else {
                                                blockLightLevel = y + 1;
                                            }
                                            if (useSkyLightlevel) {
                                                texture = mc.theWorld.getSavedLightValue(EnumSkyBlock.Sky, wx, blockLightLevel, wz); 
                                            }
                                            else {
                                                texture = mc.theWorld.getSavedLightValue(EnumSkyBlock.Block, wx, blockLightLevel, wz);
                                            }
                                            
                                            if (texture <= showLightlevelUpto) {
                                                texture += (textureRow * 16);
                                                renderer.addOverlay(wx, y, wz, blockHeight, texture);
                                            }
                                        }
                                    }
                                    else { // not valid to draw onto? must be an airblock
                                        block = null;
                                    }
                                }
                                previous = block;
                            }
                        }
                    }
                }
            }
        }
        debugMessage("generation took %dms for %d overlays", System.currentTimeMillis() - tStart, renderer.getCacheSize());
        
        renderer.stopGenerate();
    }
    
    
    private class GenerateThread extends Thread {
        public GenerateThread() {
            setName("lloverlay");
        }
        
        public void run() {
            try {
                while (true) {
                    if (active && mc.thePlayer != null) {
                        //generate(); // flood filling collecting
                        generateByChunk();
                    }
                    
                    sleep(generateInterval);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            
            debugMessage("overlay thread stopped");
        }
    }
}
