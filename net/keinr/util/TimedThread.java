package net.keinr.util;

import java.lang.InterruptedException;

/**
 * A class used for creating schedualed tasks.
 * 
 * @author Orion Musselman (KeinR)
 * @version 1.0.0
 */

public class TimedThread implements Runnable {
    private static int idLevel = 0;

    private boolean running;
    private long interval;
    private Runnable function;
    private final Thread thread;

    /**
     * Create a new TimedThread object
     * @param name the name of the thread
     * @param interval how often the thread will run in milliseconds
     * @param function and extension of the Runnable interface that will be called at the end of each interval
     */
    TimedThread(String name, long interval, Runnable function) {
        this.function = function;
        this.interval = interval;
        this.thread = new Thread(this, name);
    }

    /**
     * Create a new TimedThread object, omitting the name
     * @param interval how often the thread will run in milliseconds
     * @param function and extension of the Runnable interface that will be called at the end of each interval
     */
    TimedThread(long interval, Runnable function) {
        this.function = function;
        this.interval = interval;
        this.thread = new Thread(this, "TimedThread#"+(idLevel++));
    }

    /**
     * Sets a new function
     * @param function the function to call after each interval
     */
    public void setFunction(Runnable function) { this.function = function; }

    /**
     * Sets a new interval
     * @param interval how often to call the function, in milliseconds
     */
    public void setInterval(long interval) { this.interval = interval; }

    /** 
     * Gets the currently used function
     * @return the currently used function
     */
    public Runnable getFunction() { return function; }

    /**
     * Gets the current interval
     * @return the current interval
     */
    public long getInterval() { return interval; }

    /**
     * Get if the thread is currently running
     * @return true if the thread is running
     */
    public boolean isRunning() {
        return thread.isAlive();
    }

    /**
     * Start the thread
     * @throws IllegalThreadStateException (uncaught) if the thread is already running
     */
    public void start() {
        thread.start();
    }

    /**
     * Stop the thread
     */
    public void stop() {
        thread.interrupt();
    }

    /**
     * Implementation of the runnable interface, this controlls the timed calling of the given function
     * Can be called manually, however it would indeed be syncronous and to stop it you'd have to call
     * Thread.currentThread().interrupt() in order to stop it.
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().interrupted()) {
                Thread.sleep(interval);
                function.run();
            }
        } catch (InterruptedException e) {
            // Exit thread
        }
    }
}
