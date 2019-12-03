package net.keinr.pacman;

class ResourceLoader {
    static final Image GHOST_SPRITE, GHOST_EYES_SPRITE, GHOST_SCARED_SPRITE, MAP;
    static {
        try {
            GHOST_SPRITE = new Image();
            GHOST_EYES_SPRITE = new Image();
            GHOST_SCARED_SPRITE = new Image();
            MAP = new Image();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error: could not load resource files");
        }
    }
}


