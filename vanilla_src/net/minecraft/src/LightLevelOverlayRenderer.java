package net.minecraft.src;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

public class LightLevelOverlayRenderer {
    private static class BlockOverlay {
        public int x;
        public int y;
        public int z;
        public double yOff; // to render on halfslabs, snow etc.
        public int tex; // texture index
        public BlockOverlay(int x, int y, int z, double yOff, int tex) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yOff = yOff;
            this.tex = tex;
        }
    }
    
    private List<BlockOverlay> cache;
    private List<BlockOverlay> swapCache;
    
    private ResourceLocation textureLocation;
    
    public LightLevelOverlayRenderer() {
        cache = new ArrayList<BlockOverlay>();
        textureLocation = new ResourceLocation("lloverlay:textures/lightlevel.png");
    }
    
    public synchronized void clear() {
        cache.clear();
    }
    
    public synchronized void addOverlay(int x, int y, int z, double yOff, int tex) {
        swapCache.add(new BlockOverlay(x, y, z, yOff, tex));
    }
    
    public void startGenerate() {
        swapCache = new ArrayList<BlockOverlay>();
    }

    public void stopGenerate() {
        cache = swapCache;
    }
    
    public synchronized void render(double x, double y, double z) {

        Minecraft.getMinecraft().func_110434_K().func_110577_a(textureLocation);
        
        GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
        //GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.5F);
        //GL11.glPushMatrix();
        //GL11.glDisable(GL11.GL_ALPHA_TEST);
        //GL11.glPolygonOffset(-3.0F, -3.0F);
        //GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        //GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        //GL11.glEnable(GL11.GL_LIGHTING);
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
        //GL11.glDisable(GL11.GL_ALPHA_TEST);
        //GL11.glPolygonOffset(0.0F, 0.0F);
        //GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        //GL11.glEnable(GL11.GL_ALPHA_TEST);
        //GL11.glDepthMask(true);
        //GL11.glPopMatrix();
    }
    
    public void renderOverlay(BlockOverlay overlay) {
        Tessellator tessellator = Tessellator.instance;

        // get bounding box data (is this time consuming?)
        double boundingBoxMinX = 0.0;
        double boundingBoxMaxX = 1.0;
        double boundingBoxMinY = 0.0;
        double boundingBoxMaxY = 1.0;
        double boundingBoxMinZ = 0.0;
        double boundingBoxMaxZ = 1.0;
        
        int textureX = (overlay.tex & 15) << 4;
        int textureY = overlay.tex & 240;
        
        double var12 = ((double)textureX + boundingBoxMinX * 16.0D) / 256.0D;
        double var14 = ((double)textureX + boundingBoxMaxX * 16.0D - 0.01D) / 256.0D;
        double var16 = ((double)textureY + boundingBoxMinZ * 16.0D) / 256.0D;
        double var18 = ((double)textureY + boundingBoxMaxZ * 16.0D - 0.01D) / 256.0D;

        double var28 = overlay.x + boundingBoxMinX;
        double var30 = overlay.x + boundingBoxMaxX;
        double var32 = overlay.y + boundingBoxMinY + 0.014 + overlay.yOff;
        double var34 = overlay.z + boundingBoxMinZ;
        double var36 = overlay.z + boundingBoxMaxZ;

        // render plane
        tessellator.addVertexWithUV(var30, var32, var36, var14, var18);
        tessellator.addVertexWithUV(var30, var32, var34, var14, var16);
        tessellator.addVertexWithUV(var28, var32, var34, var12, var16);
        tessellator.addVertexWithUV(var28, var32, var36, var12, var18);
    }

}
