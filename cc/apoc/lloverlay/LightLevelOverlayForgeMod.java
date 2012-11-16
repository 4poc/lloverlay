package cc.apoc.lloverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.RenderGlobal;
import net.minecraft.src.Tessellator;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod( modid = "lloverlay", name="Light Level Overlay", version="v0.9")
public class LightLevelOverlayForgeMod {
    @Instance("LightLevelOverlayForgeMod")
    public static LightLevelOverlayForgeMod instance;
	
    @PreInit
    public void preInit(FMLPreInitializationEvent event) {
    }

    @Init
    public void load(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @PostInit
    public void postInit(FMLPostInitializationEvent event) {
    }
	
	@ForgeSubscribe
	public void renderWorldLastEvent(RenderWorldLastEvent evt) {
		Minecraft mc = Minecraft.getMinecraft();
		RenderGlobal renderGlobal = mc.renderGlobal;
		RenderBlocks globalRenderBlocks = renderGlobal.globalRenderBlocks;
		Tessellator tessellator = Tessellator.instance;
		EntityPlayer player = mc.thePlayer;
		LightLevelOverlay.instance.render(
				globalRenderBlocks, tessellator, player, evt.partialTicks);
	}
}

