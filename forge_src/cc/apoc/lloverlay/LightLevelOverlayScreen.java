package cc.apoc.lloverlay;

import net.minecraft.client.gui.GuiScreen;

public class LightLevelOverlayScreen extends GuiScreen {
    public LightLevelOverlayScreen(LightLevelOverlayConfig config) {
        super();
    }
    
    public void initGui() {
        
    }
    
    public void drawScreen(int par1, int par2, float par3) {
        
        drawCenteredString(fontRenderer, "Hello World", 100, 100, 0);
        
        
        super.drawScreen(par1, par2, par3);
    }
}
