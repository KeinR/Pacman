package net.keinr.util;

/**
 * Basic ANSI color escape sequences
 * 
 * @author Orion Musselman
 * @version 1.0.1
 */

public enum Ansi {

    // Misc

    RESET("\033[0m"),

    ITALIC("\033[3m"),
    UNDERLINE("\033[4m"),
    CROSSOUT("\033[9m"),
    
    U_ITALIC("\033[23m"),
    U_UNDERLINE("\033[24m"),
    U_CROSSOUT("\033[29m"),

    // Foreground colors

    DEFAULT("\033[39m"),

    BLACK("\033[30m"),
    RED("\033[31m"),
    GREEN("\033[32m"),
    YELLOW("\033[33m"),
    BLUE("\033[34m"),
    MAGENTA("\033[35m"),
    CYAN("\033[36m"),
    WHITE("\033[37m"),

    BR_BLACK("\033[90m"),
    BR_RED("\033[91m"),
    BR_GREEN("\033[92m"),
    BR_YELLOW("\033[93m"),
    BR_BLUE("\033[94m"),
    BR_MAGENTA("\033[95m"),
    BR_CYAN("\033[96m"),
    BR_WHITE("\033[97m");

    private final String value;

    /**
     * Called by each enum, saves a new escape sequence
     * @param value the escape sequence
     */
    private Ansi(String value) {
        this.value = value;
    }

    /**
     * Changes the string value of each enum to `value`
     * @return the value of the enum
     */
    @Override
    public String toString() {
        return this.value;
    }

    /**
     * Gets a custom ANSI squence based on a sequence of codes
     * @param codes the ANSI codes
     * @return the formatted ANSI escape sequence
     */
    public static String get(int... codes) {
        StringBuilder sb = new StringBuilder();
        for (int code : codes) {
            sb.append(String.valueOf(code)+";");
        }
        sb.deleteCharAt(sb.length()-1); // Remove last unnecessary semicolon
        return "\033["+sb.toString()+"m";
    }

    /**
     * Abstraction used for seperating foreground from background ANSI sequences
     */
    public enum bg { // breaking naming conventions just this once

        // Background colors

        DEFAULT("\033[49m"),

        BLACK("\033[40m"),
        RED("\033[41m"),
        GREEN("\033[42m"),
        YELLOW("\033[43m"),
        BLUE("\033[44m"),
        MAGENTA("\033[45m"),
        CYAN("\033[46m"),
        WHITE("\033[47m"),

        BR_BLACK("\033[100m"),
        BR_RED("\033[101m"),
        BR_GREEN("\033[102m"),
        BR_YELLOW("\033[103m"),
        BR_BLUE("\033[104m"),
        BR_MAGENTA("\033[105m"),
        BR_CYAN("\033[106m"),
        BR_WHITE("\033[107m");

        private final String value;

        /**
        * Called by each enum, saves a new (background color) escape sequence
        * @param value the escape sequence
        */
        private bg(String value) {
            this.value = value;
        }

        /**
        * Changes the string value of each enum to `value`
        * @return the value of the enum
        */
        @Override
        public String toString() {
            return this.value;
        }
    }
}
