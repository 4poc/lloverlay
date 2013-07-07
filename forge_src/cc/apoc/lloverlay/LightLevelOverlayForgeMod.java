package cc.apoc.lloverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod( modid = "lloverlay", name="Light Level Overlay", version="@VERSION@")
public class LightLevelOverlayForgeMod {
    @Instance("LightLevelOverlayForgeMod")
    public static LightLevelOverlayForgeMod instance;

    @EventHandler
    public void initialize(FMLInitializationEvent evt) {
        LightLevelOverlay.getInstance();

        MinecraftForge.EVENT_BUS.register(this);
    }
	
	@ForgeSubscribe
	public void renderWorldLastEvent(RenderWorldLastEvent evt) {
	    RenderGlobal renderGlobal = Minecraft.getMinecraft().renderGlobal;
		LightLevelOverlay.getInstance().render(renderGlobal.globalRenderBlocks, evt.partialTicks);
	}
}
