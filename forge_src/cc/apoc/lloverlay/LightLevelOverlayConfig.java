package cc.apoc.lloverlay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import net.minecraft.client.settings.GameSettings;

public class LightLevelOverlayConfig {

    private File file;
    
    
    
    /**
     * Activates debugging and profiling information.
     * 
     * Debug messages are printed to stderr.
     */
    private boolean debug;
    
    /**
     * Hotkey that toggles the overlay.
     */
    private int hotkey;
     
    private int textureRow;
    // only draw overlay for lightlevel <n> and lower.
    private int showLightlevelUpto;
    // interval in ms in which the overlay cache should be generated,
    // for instance if you place a torch its at least 250 ms till the
    // overlays are updated
    private int generateInterval;
    // overlay drawing area around the player
    private int drawChunks; // 4 chunks around the player
    
    // show the lightlevel affected by the sun
    private boolean useSkyLightlevel;
    
    // what renderer to use (slow 'vanilla', or 'fast') auto=autodetect
    public enum Renderer {
        AUTO,
        VANILLA,
        FAST
    }
    private Renderer renderer = Renderer.AUTO;
    
    public LightLevelOverlayConfig(File file) {
        this.file = file;
        if (!file.getParentFile().exists()) {
            // create config directory
            file.getParentFile().mkdir();
        }
        if (!file.exists()) {
            reset();
        }
        else {
            load();
        }
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("drawChunks", Integer.toString(drawChunks));
        properties.setProperty("hotkey", Integer.toString(hotkey));
        properties.setProperty("generateInterval", Integer.toString(generateInterval));
        properties.setProperty("textureRow", Integer.toString(textureRow));
        properties.setProperty("debug", Boolean.toString(debug));
        properties.setProperty("showLightlevelUpto", Integer.toString(showLightlevelUpto));
        properties.setProperty("useSkyLightlevel", Boolean.toString(useSkyLightlevel));
        properties.setProperty("renderer", getRendererString());
        try {
            properties.store(new FileOutputStream(file), "Lightlevel Overlay Config");
            debugMessage("config saved: %s", file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRendererString() {
        switch (renderer) {
        case FAST:
            return "fast";
        case VANILLA:
            return "vanilla";
        case AUTO:
        default:
            return "auto";
        }
    }
    
    public Renderer parseRendererString(String r) {
        Renderer renderer;
        if (r.equals("vanilla"))
            renderer = Renderer.VANILLA;
        else if (r.equals("fast"))
            renderer = Renderer.FAST;
        else
            renderer = Renderer.AUTO;
        return renderer;
    }

    public void load() {
        Properties properties = new Properties();
        try {
            file.createNewFile();
            properties.load(new FileInputStream(file));
            drawChunks = Integer.parseInt(properties.getProperty("drawChunks", "4"));
            hotkey = Integer.parseInt(properties.getProperty("hotkey", "67"));
            generateInterval = Integer.parseInt(properties.getProperty("generateInterval", "250"));
            textureRow = Integer.parseInt(properties.getProperty("textureRow", "0"));
            debug = Boolean.parseBoolean(properties.getProperty("debug", "false"));
            showLightlevelUpto = Integer.parseInt(properties.getProperty("showLightlevelUpto", "15"));
            useSkyLightlevel = Boolean.parseBoolean(properties.getProperty("useSkyLightlevel", "false"));
            renderer = parseRendererString(properties.getProperty("renderer", "auto"));
            debugMessage("config loaded: %s", file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }   
    }

    public void reset() {
        if (file.exists()) {
            file.delete();
        }
        load();
        save();
    }
    
    protected boolean isForge() {
        try {
            Class.forName ("net.minecraftforge.client.MinecraftForgeClient");
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private void debugMessage(String fmt, Object... args) {
        if (debug) {
            String message = String.format(fmt, args);
            System.err.printf("[LightLevelOverlay] %s\n", message);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int getHotkey() {
        return hotkey;
    }

    public void setHotkey(int hotkey) {
        this.hotkey = hotkey;
    }

    public String getHotkeyString() {
        return GameSettings.getKeyDisplayString(getHotkey());
    }

    public int getTextureRow() {
        return textureRow;
    }

    public void setTextureRow(int textureRow) {
        this.textureRow = textureRow;
    }

    public int getShowLightlevelUpto() {
        return showLightlevelUpto;
    }

    public void setShowLightlevelUpto(int showLightlevelUpto) {
        this.showLightlevelUpto = showLightlevelUpto;
    }

    public int getGenerateInterval() {
        return generateInterval;
    }

    public void setGenerateInterval(int generateInterval) {
        this.generateInterval = generateInterval;
    }

    public int getDrawChunks() {
        return drawChunks;
    }

    public void setDrawChunks(int drawChunks) {
        this.drawChunks = drawChunks;
    }

    public boolean isUseSkyLightlevel() {
        return useSkyLightlevel;
    }

    public void setUseSkyLightlevel(boolean useSkyLightlevel) {
        this.useSkyLightlevel = useSkyLightlevel;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }


}

