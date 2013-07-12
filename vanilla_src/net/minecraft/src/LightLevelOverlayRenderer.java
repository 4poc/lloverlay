package cc.apoc.lloverlay;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.lwjgl.opengl.GL11;

public class LightLevelOverlayRenderer {
    private static class BlockOverlay {
        public int x;
        public int y;
        public int z;
        public double blockHeight; // to render on halfslabs, snow etc.
        public int tex; // texture index
        public BlockOverlay(int x, int y, int z, double blockHeight, int tex) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockHeight = blockHeight;
            this.tex = tex;
        }
    }
    
    private List<BlockOverlay> cache;
    private List<BlockOverlay> swapCache;
    
    private ResourceLocation textureLocation;
    
    public LightLevelOverlayRenderer() {
        cache = new LinkedList<BlockOverlay>();
        textureLocation = new ResourceLocation("minecraft:textures/lightlevel.png");
    }
    
    public synchronized void clear() {
        cache.clear();
    }
    
    public synchronized void addOverlay(int x, int y, int z, double blockHeight, int tex) {
        swapCache.add(new BlockOverlay(x, y, z, blockHeight, tex));
    }
    
    public void startGenerate() {
        swapCache = new LinkedList<BlockOverlay>();
    }

    public void stopGenerate() {
        cache = swapCache;
    }
    
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * Render overlays, translated by client player position.
     * 
     * Does use the vanilla vertex array implementation internally (not VBO).
     * 
     * @param x
     * @param y
     * @param z
     */
    public synchronized void render(double x, double y, double z) {
        GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
        Minecraft.getMinecraft().func_110434_K().func_110577_a(textureLocation);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.5F);
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPolygonOffset(-3.0F, -3.0F);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setTranslation(-x, -y, -z);
        tessellator.disableColor();

        // render overlays
        for (BlockOverlay overlay : cache) {
            renderOverlay(overlay);
        }

        tessellator.draw();
        tessellator.setTranslation(0.0D, 0.0D, 0.0D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPolygonOffset(0.0F, 0.0F);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }
    
    /**
     * Renders a single plane according to the overlay.
     * 
     * Does triangulate internally (using the vanilla tessellator).
     * 
     * @param overlay
     */
    public void renderOverlay(BlockOverlay overlay) {
        Tessellator tessellator = Tessellator.instance;

        double boxMinX = 0.0;
        double boxMaxX = 1.0;
        double boxMinZ = 0.0;
        double boxMaxZ = 1.0;
        
        int textureX = (overlay.tex & 15) << 4;
        int textureY = overlay.tex & 240;
        
        // texture coordinates
        double umin = ((double)textureX + boxMinX * 16.0D) / 256.0D;
        double umax = ((double)textureX + boxMaxX * 16.0D - 0.01D) / 256.0D;
        double vmin = ((double)textureY + boxMinZ * 16.0D) / 256.0D;
        double vmax = ((double)textureY + boxMaxZ * 16.0D - 0.01D) / 256.0D;

        // plane geometry coordinates
        double xmin = overlay.x + boxMinX;
        double xmax = overlay.x + boxMaxX;
        double y = overlay.y + overlay.blockHeight + 0.014;
        double zmin = overlay.z + boxMinZ;
        double zmax = overlay.z + boxMaxZ;

        // add plane vertices
        tessellator.addVertexWithUV(xmax, y, zmax, umax, vmax);
        tessellator.addVertexWithUV(xmax, y, zmin, umax, vmin);
        tessellator.addVertexWithUV(xmin, y, zmin, umin, vmin);
        tessellator.addVertexWithUV(xmin, y, zmax, umin, vmax);
    }

}
