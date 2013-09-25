package cc.apoc.lloverlay;

import java.util.Arrays;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.gui.GuiSmallButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.EnumOptions;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

@SideOnly(Side.CLIENT)
public class LightLevelOverlaySettingsScreen extends GuiScreen {

    protected String screenTitle = "Light Level Overlay Settings";

    private GameSettings guiGameSettings;

    protected LightLevelOverlayConfig config;

    public LightLevelOverlaySettingsScreen(LightLevelOverlayConfig config) {
        this.config = config;
    }

    private static final int BUTTON_TEXTURE_ROW = 1;
    private static final int BUTTON_LIGHT_LEVEL_UPTO = 2;
    private static final int BUTTON_SKYLIGHT = 3;
    private static final int BUTTON_RELOAD = 4;
    private static final int BUTTON_HOTKEY = 5;
    private static final int BUTTON_RENDERER = 6;
    private static final int BUTTON_DRAW_CHUNKS = 7;
    private static final int BUTTON_GENERATE_INTERVAL = 8;
    private static final int BUTTON_RESET = 9;
    private static final int BUTTON_DONE = 200;
    
    private GuiButton hotkeyButton;
    private boolean waitForHotkey = false;
    
    private Integer[] range(int max) {
        Integer[] a = new Integer[16];
        for (int i=0; i<max;i++) a[i] = i;
        return a;
    }
    
    public void initGui() {
        buttonList.clear();
        
        // TODO: structure this better!/ refactor this

        // left column
        
        buttonList.add(new GuiArrayButton<Integer>(BUTTON_TEXTURE_ROW, getGridX(0), getGridY(0), "Overlay Texture: %d", 
                range(16), config.getTextureRow()));

        buttonList.add(new GuiArrayButton<Integer>(BUTTON_LIGHT_LEVEL_UPTO, getGridX(0), getGridY(1), "Show Light Upto Level: %d", 
                range(16), config.getShowLightlevelUpto()));
        
        buttonList.add(new GuiArrayButton<String>(BUTTON_SKYLIGHT, getGridX(0), getGridY(2), "Use Skylight: %s", 
                new String[] { "OFF", "ON" }, config.isUseSkyLightlevel() ? 1 : 0));
        
        addButton(BUTTON_RELOAD, 0, 6, "Reload Configuration");

        // right column
        
        hotkeyButton = new GuiSmallButton(BUTTON_HOTKEY, getGridX(1), getGridY(0), "Hotkey: " + config.getHotkeyString());
        buttonList.add(hotkeyButton);

        String[] rendererOptions = new String[] { "auto", "fast", "vanilla" };
        
        buttonList.add(new GuiArrayButton<String>(BUTTON_RENDERER, getGridX(1), getGridY(1), "Renderer: %s", 
                rendererOptions, Arrays.asList(rendererOptions).indexOf(config.getRendererString())));
        
        Integer[] drawChunksOptions = new Integer[] { 2, 4, 6, 8, 16 };
        
        buttonList.add(new GuiArrayButton<Integer>(BUTTON_DRAW_CHUNKS, getGridX(1), getGridY(2), "Render Distance: %d Chunks", 
                drawChunksOptions, Arrays.asList(drawChunksOptions).indexOf(config.getDrawChunks())));
        
        Integer[] renderIntervalOptions = new Integer[] { 150, 250, 500, 1000, 2000, 3000, 4000, 5000 };
        
        buttonList.add(new GuiArrayButton<Integer>(BUTTON_GENERATE_INTERVAL, getGridX(1), getGridY(3), "Generate Interval: %dms", 
                renderIntervalOptions, Arrays.asList(renderIntervalOptions).indexOf(config.getGenerateInterval())));
        
        addButton(BUTTON_RESET, 1, 6, "Reset to Defaults");

        buttonList.add(new GuiButton(BUTTON_DONE, this.width / 2 - 100, this.height / 6 + 168, "Done"));
    }

    @Override
    public void keyTyped(char c, int i) {
        super.keyTyped(c, i);
        if (waitForHotkey) {
            waitForHotkey = false;
            config.setHotkey(i);
            hotkeyButton.displayString = "Hotkey: " + config.getHotkeyString();
        }
    }

    private int getGridX(int x) {
        return width / 2 - 155 + x % 2 * 160;
    }

    private int getGridY(int y) {
        return height / 7 + y * 24;
    }

    private void addButton(int id, int x, int y, String caption) {
        this.buttonList.add(new GuiSmallButton(id, getGridX(x), getGridY(y), caption));
    }

    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;
        
        switch (button.id) {
        case BUTTON_TEXTURE_ROW:
            config.setTextureRow(((GuiArrayButton<Integer>) button).getValue());
            break;
        case BUTTON_LIGHT_LEVEL_UPTO:
            config.setShowLightlevelUpto(((GuiArrayButton<Integer>) button).getValue());
            break;
        case BUTTON_SKYLIGHT:
            config.setUseSkyLightlevel(((GuiArrayButton<String>) button).getValue().equals("ON"));
            break;
            
        case BUTTON_HOTKEY:
            waitForHotkey = true;
            button.displayString = "Hotkey: ???";
            break;
            
        case BUTTON_RENDERER:
            LightLevelOverlayConfig.Renderer r = 
                config.parseRendererString((((GuiArrayButton<String>) button).getValue()));
            config.setRenderer(r);
            break;
        case BUTTON_DRAW_CHUNKS:
            config.setDrawChunks(((GuiArrayButton<Integer>) button).getValue());
            break;
        case BUTTON_GENERATE_INTERVAL:
            config.setGenerateInterval(((GuiArrayButton<Integer>) button).getValue());
            break;
            
        case BUTTON_RESET:
            debugMessage("reset config");
            config.reset();
            LightLevelOverlay.getInstance().reload();
            initGui();
            break;
            
        case BUTTON_RELOAD:
            debugMessage("reload config");
            config.load();
            LightLevelOverlay.getInstance().reload();
            initGui();
            break;
            
        case BUTTON_DONE:
        default:
            config.save();
            this.mc.displayGuiScreen((GuiScreen) null);
            this.mc.setIngameFocus();
        }
    }

    public void drawScreen(int par1, int par2, float par3) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2, 20, 16777215);
        super.drawScreen(par1, par2, par3);
    }

    private static class GuiArrayButton<T> extends GuiButton {
        private T[] array;
        private int initialIndex;
        private int currentIndex;
        private String captionFormat;

        public GuiArrayButton(int id, int posX, int posY, String captionFormat, T[] array) {
            this(id, posX, posY, captionFormat, array, 0);
        }

        public GuiArrayButton(int id, int posX, int posY, String captionFormat, T[] array, int initialIndex) {
            super(id, posX, posY, 150, 20, "<unset>");
            this.array = array;
            this.initialIndex = currentIndex = (initialIndex > 0 && initialIndex < array.length) ? initialIndex : 0;
            this.captionFormat = captionFormat;
            
            this.displayString = getCaption();
        }

        public T getValue() {
            return array[currentIndex];
        }

        private String getCaption() {
            return String.format(captionFormat, array[currentIndex]);
        }

        public boolean mousePressed(Minecraft par1Minecraft, int par2, int par3) {
            if (super.mousePressed(par1Minecraft, par2, par3)) {

                if (Mouse.isButtonDown(0)) {
                    // switch through array elements:
                    currentIndex = currentIndex < array.length - 1 ? currentIndex + 1 : 0;
                }
                else {
                    // reset:
                    currentIndex = initialIndex;
                }

                this.displayString = getCaption();

                return true;
            }
            else {
                return false;
            }
        }
    }
    
    private void debugMessage(String fmt, Object... args) {
        if (config.isDebug()) {
            String message = String.format(fmt, args);
            System.err.printf("[LightLevelOverlay] %s\n", message);
        }
    }
}
