package cc.apoc.lloverlay;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

public class LightLevelOverlayThread extends Thread {

    
    

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
    
    
    private LightLevelOverlayConfig config;
    private LightLevelOverlayRenderer renderer;
    
    private Minecraft mc;
    private RenderBlocks renderBlocks;

    
    private boolean active;




    public LightLevelOverlayThread(LightLevelOverlayConfig config, LightLevelOverlayRenderer renderer) {
        this.config = config;
        this.renderer = renderer; 
        setName("lloverlay");
        mc = Minecraft.getMinecraft();
    }
    
    
    
    public boolean isOverlayBlock(Block block) {
        if (block == null)
            return false;
        
        for (int id : OVERLAY_BLOCKS)
            if (block.blockID == id)
                return true;
        return false;
    }

    
    public void run() {
        try {
            while (true) {
                if (active && mc.thePlayer != null) {
                    generateByChunk();
                }
                
                sleep(config.getGenerateInterval());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        debugMessage("overlay thread stopped");
    }

    
    

    private void generateByChunk() throws Exception {
        if (renderBlocks == null) return;
        // indicates the renderer to get a new set of overlays
        renderer.startGenerate();

        long tStart = System.currentTimeMillis();

        int playerY = (int) Math.floor(mc.thePlayer.posY);
        
        int playerChunkX = mc.thePlayer.chunkCoordX;
        int playerChunkZ = mc.thePlayer.chunkCoordZ;
        
        debugMessage("start generation by chunk");

        int distance = config.getDrawDistance();
        int chunkDistance = (int) Math.ceil(distance / 16.0);
        
        // collect block & lighting information per chunks around the player
        IChunkProvider provider = mc.theWorld.getChunkProvider();
        for (int chunkX = playerChunkX - chunkDistance; chunkX <= playerChunkX + chunkDistance; chunkX++) {
            for (int chunkZ = playerChunkZ - chunkDistance; chunkZ <= playerChunkZ + chunkDistance; chunkZ++) {
                if (provider.chunkExists(chunkX, chunkZ)) {
                    Chunk chunk = provider.provideChunk(chunkX, chunkZ);
                    
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            Block previous = null;
                            // begin iteration above the players head (small optimization)
                            for (int y = playerY+3; y > playerY+3-distance && y > 0; y--) {
                                
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
                                            if (config.isUseSkyLightlevel()) {
                                                texture = mc.theWorld.getSavedLightValue(EnumSkyBlock.Sky, wx, blockLightLevel, wz); 
                                            }
                                            else {
                                                texture = mc.theWorld.getSavedLightValue(EnumSkyBlock.Block, wx, blockLightLevel, wz);
                                            }
                                            
                                            if (texture <= config.getShowLightlevelUpto()) {
                                                texture += (config.getTextureRow() * 16);
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

    public void setRenderBlocks(RenderBlocks renderBlocks) {
        this.renderBlocks = renderBlocks;
    }    
    
    
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    private void debugMessage(String fmt, Object... args) {
        if (config.isDebug()) {
            String message = String.format(fmt, args);
            System.err.printf("[LightLevelOverlay] %s\n", message);
        }
    }



    
}
