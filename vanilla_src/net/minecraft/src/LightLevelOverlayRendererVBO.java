package net.minecraft.src;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

public class LightLevelOverlayRendererVBO implements LightLevelOverlayRenderer {
    
    private ResourceLocation textureLocation;
    
    private LightLevelOverlayConfig config;
    
    int vertexId = 0;
    int texcoordId = 0;
    
    private FloatBuffer vertexBuffer;
    private FloatBuffer texcoordBuffer;
    
    private float[] vertexArray;
    private float[] texcoordArray;
    
    private int vertexArrayIndex = 0;
    private int texcoordArrayIndex = 0;
    
    private int vertices = 0;
    
    boolean needUpload = false;
    
    public LightLevelOverlayRendererVBO(LightLevelOverlayConfig config) {
        this.config = config;
        
        textureLocation = new ResourceLocation("minecraft:textures/lightlevel.png");
        
        // init buffers used for the vbo
        vertexBuffer = createBuffer(((int) Math.pow(config.getDrawDistance() * 2, 3) + 1) * 3);
        texcoordBuffer = createBuffer(((int) Math.pow(config.getDrawDistance() * 2, 3) + 1) * 2);
        
        vertexArray = new float[vertexBuffer.capacity()];
        texcoordArray = new float[texcoordBuffer.capacity()];
        
        vertexId = GL15.glGenBuffers();
        texcoordId = GL15.glGenBuffers();
    }
        
    public synchronized void clear() {
        vertexArrayIndex = 0;
        texcoordArrayIndex = 0;
    }
    
    public synchronized void addOverlay(int x, int y, int z, double blockHeight, int tex) {
        double boxMinX = 0.0;
        double boxMaxX = 1.0;
        double boxMinZ = 0.0;
        double boxMaxZ = 1.0;
        
        int textureX = (tex & 15) << 4;
        int textureY = tex & 240;
        
        // texture coordinates
        float umin = (float) (((double)textureX + boxMinX * 16.0D) / 256.0f);
        float umax = (float) (((double)textureX + boxMaxX * 16.0D - 0.01D) / 256.0f);
        float vmin = (float) (((double)textureY + boxMinZ * 16.0D) / 256.0f);
        float vmax = (float) (((double)textureY + boxMaxZ * 16.0D - 0.01D) / 256.0f);

        // plane geometry coordinates
        float xmin = (float) (x + boxMinX);
        float xmax = (float) (x + boxMaxX);
        float ycoord = (float) (y + blockHeight + 0.014);
        float zmin = (float) (z + boxMinZ);
        float zmax = (float) (z + boxMaxZ);

        // add plane vertices
        addVertex(xmax, ycoord, zmax);
        addTexUV(umax, vmax);
        addVertex(xmax, ycoord, zmin);
        addTexUV(umax, vmin);
        addVertex(xmin, ycoord, zmin);
        addTexUV(umin, vmin);

        addVertex(xmax, ycoord, zmax);
        addTexUV(umax, vmax);
        addVertex(xmin, ycoord, zmin);
        addTexUV(umin, vmin);
        addVertex(xmin, ycoord, zmax);
        addTexUV(umin, vmax);
    }
    
    private void addVertex(float x, float y, float z) {
        vertexArray[vertexArrayIndex++] = x;
        vertexArray[vertexArrayIndex++] = y;
        vertexArray[vertexArrayIndex++] = z;
    }
    
    private void addTexUV(float u, float v) {
        texcoordArray[texcoordArrayIndex++] = u;
        texcoordArray[texcoordArrayIndex++] = v;
    }
    
    public void startGenerate() {
        clear();
    }

    public void stopGenerate() {

        vertexBuffer.limit(vertexArrayIndex);
        vertexBuffer.put(vertexArray, 0, vertexArrayIndex);
        vertexBuffer.position(0);
        
        texcoordBuffer.limit(texcoordArrayIndex);
        texcoordBuffer.put(texcoordArray, 0, texcoordArrayIndex);
        texcoordBuffer.position(0);
        
        vertices = vertexArrayIndex;
        needUpload = true;
    }
    
    private void uploadBuffers() {
        debugMessage("upload VBO");
        long tStart = System.currentTimeMillis();
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexArrayIndex, GL15.GL_STATIC_DRAW);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, texcoordArrayIndex, GL15.GL_STATIC_DRAW);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, texcoordBuffer, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        
        needUpload = false;
        debugMessage("VBO upload took %dms for %d vertices.", System.currentTimeMillis() - tStart, vertices);
        
    }
    
    private void renderVBO() {
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexId);
        GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordId);
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);

        //System.out.println(vertices);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertices);
        
        // disable the VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }
    
    public int getCacheSize() {
        return (int) vertexArrayIndex / 3;
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
        if (needUpload) {
            uploadBuffers();
        }
        
        if (vertices == 0) return;
        
        GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
        Minecraft.getMinecraft().func_110434_K().func_110577_a(textureLocation);
        
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.5F);
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPolygonOffset(-3.0F, -3.0F);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glTranslated(-x, -y, -z);

        // render overlays
        renderVBO();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPolygonOffset(0.0F, 0.0F);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }
    
    private FloatBuffer createBuffer(int size) {
        ByteBuffer vbb = ByteBuffer.allocateDirect(size * 4); 
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = vbb.asFloatBuffer();
        fb.position(0);
        
        return fb;
    }
    
    private void debugMessage(String fmt, Object... args) {
        if (config.isDebug()) {
            String message = String.format(fmt, args);
            System.err.printf("[LightLevelOverlay] %s\n", message);
        }
    }
}
