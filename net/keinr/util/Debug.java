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
 * @version 2.0.0
 */

public class Debug {
    private Pattern shorthand;
    private boolean debug;
    private String name;

    /**
     * Create a new instance, while setting the shorthand
     * @param shorthand the regex string to be used as the shorthand
     */
    public Debug(String shorthand) {
        this.shorthand = Pattern.compile(shorthand);
        this.name = name;
        this.debug = debug;
    }

    /**
     * Create a new instance, while setting the debug status
     * @param debug debug status
     */
    public Debug(boolean debug) {
        this.shorthand = Pattern.compile("");
        this.name = "";
        this.debug = debug;
    }

    /**
     * Create a new instance, while setting the shorthand & debug status
     * @param name name that will be displayed when logging
     * @param debug debug status
     */
    public Debug(String shorthand, boolean debug) {
        this.shorthand = Pattern.compile(shorthand);
        this.name = "";
        this.debug = debug;
    }

    /**
     * Create a new instance, while setting the shorthand, display name & debug status
     * @param shorthand the regex string to be used as the shorthand
     * @param name name that will be displayed when logging
     * @param debug debug status
     */
    public Debug(String shorthand, String name, boolean debug) {
        this.shorthand = Pattern.compile(shorthand);
        this.name = name;
        this.debug = debug;
    }

    /**
     * Create a new instance with no parameters
     */
    public Debug() {
        this.shorthand = Pattern.compile("");
        this.name = "";
        this.debug = true;
    }

    /**
     * Sets the display name for this debug object
     * @param value the new value
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Toggles debug output for this instance
     * @param toggle debug state
     */
    public void toggleDebug(boolean toggle) { debug = toggle; }

    /**
     * Sets the shorthand, a REGEX that will be compared to the logging class's name
     * and have all its matches removed.
     * @param regex the new shorthand. Keep in mind that this is a regex, so be sure to make it as efficient as possible.
     *                     For example, instead of `com\\.example`, do `^com\\.example`.
     */
    public void setShorthand(String regex) {
        shorthand = Pattern.compile(regex); // Precompiling to improve performance
    }

    /**
     * Shorthand getter
     * @return the current shorthand
     */
    public String getShorthand() { return shorthand.pattern(); }

    /**
     * Gets the debug state for this instance
     * @return is logging toggled?
     */
    public boolean getDebug() { return debug; }

    /**
     * Gets the current name of this object
     * @return current name value
     */
    public String getName() { return name; }

    /**
     * Logs a string depending on the debug status of this instance
     * @param message the message to log
     */
    public void log(String message) {
        logRouted(message, 1);
    }

    /**
     * Logs a string depending on the debug status of this instance, and checks past a routing function
     * by specifying a "distance" paremeter.
     * For example, if you have just one method that is wrapping Debug#log(String,int), you specify a distance of 1, as
     * you want to ignore one entree in the stack trace, the wrapper function that called Debug#log(String,int).
     * @param distance number of stack trace entrees to ignore
     * @param message the message to log
     */
    public void logRouted(String message, int distance) {
        if (debug) {
            StackTraceElement call = Thread.currentThread().getStackTrace()[2+distance]; // Get stacktrace for where function was called
            System.out.println("["+GREEN+(name.length()!=0?name:"debug")+CYAN+"@"+shorthand.matcher(call.getClassName()).replaceAll("")+":"+call.getLineNumber()+RESET+"] "+message);
        }
    }

    /**
     * Runs a function depending on the debug status of this instance
     * @param function the function to run, implementation of the Runnable interface
     */
    public void logFunction(Runnable function) {
        logFunctionRouted(function, 1);
    }

    /**
     * Runs a routed function depending on the debug status of this instance
     * @param distance how many stack trace entrees to ignore
     * @param function the function to run, implementation of the Runnable interface
     */
    public void logFunctionRouted(Runnable function, int distance) {
        if (debug) {
            StackTraceElement call = Thread.currentThread().getStackTrace()[2];
            System.out.println(
                GREEN+"================= start "+(name.length()!=0?name:"debug")+CYAN+"@"+
                shorthand.matcher(call.getClassName()).replaceAll("")+":"+call.getLineNumber()+
                GREEN+" function ================="+RESET);

            function.run();
            
            System.out.println(GREEN+"======================== end debug function ========================="+RESET);
        }
    }
}
