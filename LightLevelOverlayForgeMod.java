package net.minecraft.src;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;

/**
 * This is just a test using Minecraft Forge #199
 * http://jenkins.minecraftforge.net:7070/job/forge/lastBuild/
 * So I guess this will be useful as soon as optifine is compatible with it
 */
@Mod( modid = "lloverlay", name="Light Level Overlay", version="v0.7")
public class LightLevelOverlayForgeMod {
	@PostInit
	public void postInit(FMLPostInitializationEvent evt) {
		MinecraftForge.EVENT_BUS.register(this);
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

