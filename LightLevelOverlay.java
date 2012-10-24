package net.minecraft.src;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;

/**
 * Minecraft mod, renders light level on top of blocks.
 * 
 * This Minecraft mod renders the light level on top of blocks around the player,
 * this helps to identify areas where mobs can spawn.
 * 
 * In vanilla this class is invoked in RenderGlobal at the end of drawBlockDamageTexture(),
 * in forge the event hook RenderWorldLastEvent can be used.
 * 
 * place at end of RenderGlobal.drawBlockDamageTexture (func_72717_a in older mcp versions):
 *
 * 	LightLevelOverlay.instance.render(globalRenderBlocks, par1Tessellator, par2EntityPlayer, par3);
 *
 * @author apoc <http://apoc.cc>
 * @version v0.8-mc1_4_1 [24.10.2012]
 * @license 3-clause BSD
 */
class LightLevelOverlay {
    public static LightLevelOverlay instance = new LightLevelOverlay();

    // debug messages are printed to stderr
    private boolean debug = false;
    // manual keypress event handling for the hotkey
    private long lastHotkeyKeydown;
    // toggles the overlay on/off
    private int hotkey = Keyboard.KEY_F9;
    // whenever the overlay should be rendered
    private boolean active = false;
    // last time the overlay cache was generated
    private long lastGenerate;
    // point in time of the frame
    private long frameTime;
    // the lightlevel.png texture file contains 
    private int textureRow = 0;
    private boolean showIlluminated = true;
    // interval in ms in which the overlay cache should be generated,
    // for instance if you place a torch its at least 250 ms till the
    // overlays are updated
    private int generateInterval = 250;
    // overlay drawing area around the player in blocks in each direction
    private int drawDistance = 25; // actual area: drawDistance^3*2
    
    private Minecraft mc;

    private File configFile;
    private Properties configProperties;

    private LightLevelOverlay() {
        debugMessage("loading");

        mc = Minecraft.getMinecraft();

        configFile = new File(mc.getMinecraftDir(), "lloverlay.properties");
        if (!configFile.exists()) {
            saveConfig(); // saves default config
        }
        loadConfig();
    }

    private void saveConfig() {
        configProperties = new Properties();
        configProperties.setProperty("drawDistance", Integer.toString(drawDistance));
        configProperties.setProperty("hotkey", Integer.toString(hotkey));
        configProperties.setProperty("generateInterval", Integer.toString(generateInterval));
        configProperties.setProperty("textureRow", Integer.toString(textureRow));
        configProperties.setProperty("debug", Boolean.toString(debug));
        configProperties.setProperty("showIlluminated", Boolean.toString(showIlluminated));
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
            if (configProperties.containsKey("showIlluminated")) {
                showIlluminated = Boolean.parseBoolean(configProperties.getProperty("showIlluminated"));
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


    public void render(RenderBlocks renderBlocks, Tessellator tessellator, EntityPlayer player, float partialTickTime) {
        frameTime = System.currentTimeMillis();
        hotkeyPoll();
        if(!active) return;

        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTickTime;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTickTime;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTickTime;

        Position playerPosition = new Position((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));

        if (cache.size() == 0 || !playerPosition.equals(cachePosition)
                || frameTime - lastGenerate > generateInterval) {
            generate(playerPosition);
        }

        GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
        int i = mc.renderEngine.getTexture("/lightlevel.png");
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, i);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.5F);
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPolygonOffset(-3.0F, -3.0F);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        tessellator.startDrawingQuads();
        tessellator.setTranslation(-x, -y, -z);
        tessellator.disableColor();

        // render overlays
        for (Position entry : cache) {
            int texture = entry.lightlevel();
            if (!showIlluminated && texture > 7) {
                continue;
            }
            texture += (textureRow * 16);

            // is snow above?
            if (entry.isSnowAbove()) {
            	renderTopFace(Block.snow, entry.x, entry.y + 1, entry.z, texture);
            }
            else {
            	renderTopFace(Block.stone, entry.x, entry.y, entry.z, texture);
            }
            
            // debugMessage("y-2 blockId = %d (spawnable: %s collidable: %s)\n", 
            //		playerPosition.getBlockId(0, -1, 0),
            //		playerPosition.isSpawnable(0, -2, 0), 
            // 		playerPosition.isCollidable(0, -2, 0));
        }

        tessellator.draw();
        tessellator.setTranslation(0.0D, 0.0D, 0.0D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPolygonOffset(0.0F, 0.0F);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }
    
    public void renderTopFace(Block block, double x, double y, double z, int texture)
    {
        Tessellator tessellator = Tessellator.instance;

        // get bounding box data (is this time consuming?)
    	double boundingBoxMinX = block.func_83009_v();
    	double boundingBoxMaxX = block.func_83007_w();
    	double boundingBoxMinY = block.func_83008_x();
    	double boundingBoxMaxY = block.func_83010_y();
    	double boundingBoxMinZ = block.func_83005_z();
    	double boundingBoxMaxZ = block.func_83006_A();

    	int var10 = (texture & 15) << 4;
        int var11 = texture & 240;
        double var12 = ((double)var10 + boundingBoxMinX * 16.0D) / 256.0D;
        double var14 = ((double)var10 + boundingBoxMaxX * 16.0D - 0.01D) / 256.0D;
        double var16 = ((double)var11 + boundingBoxMinZ * 16.0D) / 256.0D;
        double var18 = ((double)var11 + boundingBoxMaxZ * 16.0D - 0.01D) / 256.0D;

        if (boundingBoxMinX < 0.0D || boundingBoxMaxX > 1.0D)
        {
            var12 = (double)(((float)var10 + 0.0F) / 256.0F);
            var14 = (double)(((float)var10 + 15.99F) / 256.0F);
        }

        if (boundingBoxMinZ < 0.0D || boundingBoxMaxZ > 1.0D)
        {
            var16 = (double)(((float)var11 + 0.0F) / 256.0F);
            var18 = (double)(((float)var11 + 15.99F) / 256.0F);
        }

        double var20 = var14;
        double var22 = var12;
        double var24 = var16;
        double var26 = var18;

        double var28 = x + boundingBoxMinX;
        double var30 = x + boundingBoxMaxX;
        double var32 = y + boundingBoxMaxY;
        double var34 = z + boundingBoxMinZ;
        double var36 = z + boundingBoxMaxZ;

        tessellator.addVertexWithUV(var30, var32, var36, var14, var18);
        tessellator.addVertexWithUV(var30, var32, var34, var20, var24);
        tessellator.addVertexWithUV(var28, var32, var34, var12, var16);
        tessellator.addVertexWithUV(var28, var32, var36, var22, var26);
    }

    private void hotkeyPoll() {
        if (Keyboard.isKeyDown(hotkey) && frameTime - lastHotkeyKeydown > 250) {
            lastHotkeyKeydown = frameTime;
            active = (active) ? false : true; // toggle!
            debugMessage("toggle active: %s", active);
            cache.clear();
        }
    }

    private void debugMessage(String fmt, Object... args) {
        if (debug) {
            String message = String.format(fmt, args);
            System.err.printf("[%s] - LightlevelOverlay - %s\n", (new Date()).toString(), message);
        }
    }

    class Position {
        public int x;

        public int y;

        public int z;

        public Position() {}

        public Position(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Position(Position o) {
            this.x = o.x;
            this.y = o.y;
            this.z = o.z;
        }
        
        public Position(Position o, int dx, int dy, int dz) {
            this.x = o.x + dx;
            this.y = o.y + dy;
            this.z = o.z + dz;
        }
        
        public int getBlockId(int dx, int dy, int dz) {
            return mc.theWorld.getBlockId(x + dx, y + dy, z + dz);
        }
        
        public boolean isSnowAbove() {
        	int blockId = getBlockId(0, 1, 0);
        	return blockId == Block.snow.blockID;
        }
        
        public boolean isSpawnable(int dx, int dy, int dz) {
            int blockId = getBlockId(dx, dy, dz);
            Block block = Block.blocksList[blockId];

            if (blockId > 0 && block.isOpaqueCube())
                return true;
            
            if (mc.theWorld.doesBlockHaveSolidTopSurface(x + dx, y + dy, z + dz)) {
                return true;
            }
            
            return false;
        }
        public boolean isCollidable(int dx, int dy, int dz) {
            int blockId = getBlockId(dx, dy, dz);
            return (blockId > 0 && Block.blocksList[blockId].isCollidable());
        }

        // light level of the (hopefully) airspace above it
        public int lightlevel() { // cache it maybe
            return mc.theWorld.getSavedLightValue(EnumSkyBlock.Block, x, y + 1, z);
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

    private Position cachePosition = new Position();
    
    private static List<Position> cache = new Vector<Position>();

    private void generate(Position playerPosition) {
        cache.clear();
        Position pos = new Position();
        boolean previous = false;
        for (int x = -drawDistance; x < drawDistance; x++)
                for (int z = -drawDistance; z < drawDistance; z++)
                    for (int y = -drawDistance; y < drawDistance; y+=2) {
                    pos.x = playerPosition.x + x;
                    pos.y = playerPosition.y + y;
                    pos.z = playerPosition.z + z;
                    
                    boolean first = pos.isSpawnable(0, 0, 0);
                    boolean second = pos.isSpawnable(0, 1, 0);
                    
                    if (first && !second) {
                        cache.add(new Position(pos));
                    }
                    else if (!first && previous) {
                        cache.add(new Position(pos, 0, -1, 0));
                    }
                    
                    previous = second;
                }
        cachePosition = playerPosition;
        lastGenerate = System.currentTimeMillis();
    }
}
