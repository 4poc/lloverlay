package net.minecraft.src;

public interface LightLevelOverlayRenderer {
    
    public void clear();
    
    public void addOverlay(int x, int y, int z, double blockHeight, int tex);
    
    public void startGenerate();
    public void stopGenerate();
    
    public int getCacheSize();
    
    public void render(double x, double y, double z);

}
