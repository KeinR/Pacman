package net.keinr.pacman;

import java.io.FileInputStream;
import javafx.scene.image.Image;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * Central location for loading & storing external resources
 * @author Orion Musselman (KeinR)
 */

final class Resource {
    private static final Map<String, Image> images = new HashMap<String, Image>();
    private static final Map<String, FileInputStream> files = new HashMap<String, FileInputStream>();
    static {
        try {
            images.put("ghost", new Image(new FileInputStream("resources/images/sprites/ghost.png")));
            images.put("ghostEyes", new Image(new FileInputStream("resources/images/sprites/eyes.png")));
            images.put("ghostScared", new Image(new FileInputStream("resources/images/sprites/scared.png")));
            images.put("icon", new Image(new FileInputStream("resources/images/icon.png")));
            files.put("map", new FileInputStream("resources/images/map.png"));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error: could not load resource files");
        }
    }
    static Image getImage(String name) {
        Image image = images.get(name);
        if (image == null) throw new ResourceNotFoundException(name, "image");
        return image;
    }
    static FileInputStream getFile(String name) {
        FileInputStream file = files.get(name);
        if (file == null) throw new ResourceNotFoundException(name, "file");
        return file;
    }

    // Doesn't quite meet the requirements for being an Error, but I don't intend on this being caught
    private static class ResourceNotFoundException extends RuntimeException {
        private ResourceNotFoundException(String givenName, String target) {
            super("Resource of name \""+givenName+"\" does not exist (is not loaded) in the \""+target+"\" map");
        }
    }
}


