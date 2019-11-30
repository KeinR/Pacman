package net.keinr.util;

import java.util.regex.Pattern;

import static net.keinr.util.Ansi.RESET;
import static net.keinr.util.Ansi.CYAN;
import static net.keinr.util.Ansi.GREEN;

/**
 * Provides methods that allow better debug logging.
 * Logs a string, allong with the class & line number
 * that the function was called using the stack trace.
 * @requires Ansi
 * 
 * @author Orion Musselman (KeinR)
 * @version 1.0.0
 */

public class Debug {

    private static boolean debugStatic = true;
    private static String shorthandRaw = "";
    private static Pattern shorthand = Pattern.compile("");

    private boolean debug = true;

    /**
     * Create a new instance, while setting the debug status
     * @param debug debug status
     */
    public Debug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Create a new instance with no parameters
     */
    public Debug() {}

    /**
     * Toggles debug output for this instance
     * @param toggle debug state
     */
    public void iToggleDebug(boolean toggle) { this.debug = toggle; }

    /**
     * Gets the debug state for this instance
     * @return is logging toggled?
     */
    public boolean iGetDebug() { return debug; }

    /**
     * Logs a string depending on the debug status of this instance
     * @param message the message to log
     */
    public void log(String message) {
        if (debug) debugText(message);
    }

    /**
     * Runs a function depending on the debug status of this instance
     * @param function the function to run, implementation of the Runnable interface
     */
    public void func(Runnable function) {
        if (debug) debugFunction(function);
    }

    // Static methods

    /**
     * Sets the static debug status
     * @param toggle new debug status
     */
    public static void toggleDebug(boolean toggle) { debugStatic = toggle; }

    /**
     * Gets the static debug status
     * @return is logging toggled?
     */
    public static boolean getDebug() { return debugStatic; }

    /**
     * Sets the shorthand, a REGEX that will be compared to the logging class's name
     * and have all its matches removed.
     * This is soley static so as to preserve the main purpose of this class:
     * To know exactly where debug strings are being logged from.
     * Therefore, it wouldn't make sense to specify non-static shorthands as that'd
     * introduce the possibility of two classes in different packages with the same name
     * using two different Debug instances with shorthands that make it seem like they're the
     * same class.
     * @param newShorthand the new shorthand. Keep in mind, this is a regex, so be sure to make it as efficient as possible.
     *                     For example, instead of `com\\.example`, do `^com\\.example`.
     */
    public static void setShorthand(String regex) {
        shorthand = Pattern.compile(regex); // Precompiling to improve performance
    }

    /**
     * Shorthand getter
     * @return the current shorthand
     */
    public static String getShorthand() { return shorthand.pattern(); }

    /**
     * Logs a string depending on the static debug status.
     * Is intended to be statically imported to save time, hence
     * the descriptive name.
     * @param message the message to log
     */
    public static void logDebug(String message) {
        if (debugStatic) debugText(message);
    }

    /**
     * Runs a function depending on the static debug status.
     * Is intended to be statically imported to save time, like logDebug(String).
     * @param function the function to run, implementation of the Runnable interface
     */
    public static void fDebug(Runnable function) {
        if (debugStatic) debugFunction(function);
    }

    // Internal

    /**
     * Internal function does the work of logging a debug string
     * @param message the message to log
     */
    private static void debugText(String message) {
        StackTraceElement call = Thread.currentThread().getStackTrace()[3]; // Get stacktrace for where function was called
        System.out.println("["+GREEN+"debug"+CYAN+"@"+shorthand.matcher(call.getClassName()).replaceAll("")+":"+call.getLineNumber()+RESET+"] "+message);
    }

    /**
     * Internal function does the work of running a function
     * @param function the function to run
     */
    private static void debugFunction(Runnable function) {
        StackTraceElement call = Thread.currentThread().getStackTrace()[3];
        System.out.println(
            GREEN+"================= start debug"+CYAN+"@"+
            shorthand.matcher(call.getClassName()).replaceAll("")+":"+call.getLineNumber()+
            GREEN+" function ================="+RESET);

        function.run();
        
        System.out.println(GREEN+"======================== end debug function ========================="+RESET);
    }
}

