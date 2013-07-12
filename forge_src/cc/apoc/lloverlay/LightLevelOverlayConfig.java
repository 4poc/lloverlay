package cc.apoc.lloverlay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;

public class LightLevelOverlayConfig {

    private File file;
    
    
    
    /**
     * Activates debugging and profiling information.
     * 
     * Debug messages are printed to stderr.
     */
    private boolean debug = false;
    
    /**
     * Hotkey that toggles the overlay.
     */
    private int hotkey = Keyboard.KEY_F9;
     
    private int textureRow = 0;
    // only draw overlay for lightlevel <n> and lower.
    private int showLightlevelUpto = 15;
    // interval in ms in which the overlay cache should be generated,
    // for instance if you place a torch its at least 250 ms till the
    // overlays are updated
    private int generateInterval = 250;
    // overlay drawing area around the player in blocks in each direction
    private int drawDistance = 25; // actual area: drawDistance^3*2
    
    // show the lightlevel affected by the sun
    private boolean useSkyLightlevel = false;
    
    public LightLevelOverlayConfig(File file) {
        this.file = file;
        if (!file.getParentFile().exists()) {
            // create config directory
            file.getParentFile().mkdir();
        }
        if (!file.exists()) {
            save(); // saves default config
        }
        load();
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("drawDistance", Integer.toString(drawDistance));
        properties.setProperty("hotkey", Integer.toString(hotkey));
        properties.setProperty("generateInterval", Integer.toString(generateInterval));
        properties.setProperty("textureRow", Integer.toString(textureRow));
        properties.setProperty("debug", Boolean.toString(debug));
        properties.setProperty("showLightlevelUpto", Integer.toString(showLightlevelUpto));
        properties.setProperty("useSkyLightlevel", Boolean.toString(useSkyLightlevel));
        try {
            properties.store(new FileOutputStream(file), "Lightlevel Overlay Config");
            debugMessage("config saved: %s", file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));
            drawDistance = Integer.parseInt(properties.getProperty("drawDistance"));
            hotkey = Integer.parseInt(properties.getProperty("hotkey"));
            generateInterval = Integer.parseInt(properties.getProperty("generateInterval"));
            textureRow = Integer.parseInt(properties.getProperty("textureRow"));
            debug = Boolean.parseBoolean(properties.getProperty("debug"));
            // for backwards-compat.:
            if (properties.containsKey("showLightlevelUpto")) {
                showLightlevelUpto = Integer.parseInt(properties.getProperty("showLightlevelUpto"));
            }
            else {
                save();
            }
            if (properties.containsKey("useSkyLightlevel")) {
                useSkyLightlevel = Boolean.parseBoolean(properties.getProperty("useSkyLightlevel"));
            }
            else {
                save();
            }
            debugMessage("config loaded: %s", file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

    public int getDrawDistance() {
        return drawDistance;
    }

    public void setDrawDistance(int drawDistance) {
        this.drawDistance = drawDistance;
    }

    public boolean isUseSkyLightlevel() {
        return useSkyLightlevel;
    }

    public void setUseSkyLightlevel(boolean useSkyLightlevel) {
        this.useSkyLightlevel = useSkyLightlevel;
    }
}

