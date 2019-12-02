package net.keinr.util;

import java.lang.InterruptedException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * A class used for running tasks at specified intervals
 * 
 * @author Orion Musselman (KeinR)
 * @version 1.0.1
 */

public class TimedThread implements Runnable {
    private static int idLevel = 0;

    private long interval;
    private Runnable function;
    private String name;

    private Thread currentThread;
    private boolean threadRunning = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(this::threadFactory);

    /**
     * Create a new TimedThread object
     * @param name the name of the thread
     * @param interval how often the thread will run in milliseconds
     * @param function and extension of the Runnable interface that will be called at the end of each interval
     */
    public TimedThread(String name, int interval, Runnable function) {
        this.function = function;
        // I do this because I will never need to pause execution of a thread for more than 22 or so days, so taking long parameters is really quite excessive
        this.interval = (long)interval;
        this.name = name;
    }

    /**
     * Create a new TimedThread object, omitting the name
     * @param interval how often the thread will run in milliseconds
     * @param function and extension of the Runnable interface that will be called at the end of each interval
     */
    public TimedThread(int interval, Runnable function) {
        this.function = function;
        this.interval = (long)interval;
        this.name = "TimedThread#"+(idLevel++);
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
    public void setInterval(int interval) { this.interval = (long)interval; }

    /**
     * Gives the thread and all future versions a new name
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
        this.currentThread.setName(name);
    }

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
    public boolean isRunning() { return threadRunning; }

    /**
     * Start the thread
     * @throws IllegalTimedThreadStateException if the thread IS still running
     */
    public void start() {
        if (threadRunning) throw new IllegalTimedThreadStateException("Thread is running");
        executor.execute(this);
        threadRunning = true;
    }

    /**
     * Stop the thread
     * @throws IllegalTimedThreadStateException if the thread ISN'T still running
     */
    public void stop() {
        if (!threadRunning) throw new IllegalTimedThreadStateException("No Thread is running");
        // I use Thread#interrupt() instead of using a boolean flag in the class so that if the sleep time is long, I can use the
        // InterruptedException to wake up the thread and have it exit
        currentThread.interrupt();
        // I have a second boolean flag so I don't have to check for null
        threadRunning = false;
    }

    /**
     * Implementation of the runnable interface, this controlls the timed calling of the given function
     * Can be called manually, however it would indeed be syncronous and to stop it you'd have to call
     * Thread.currentThread().interrupt() in order to stop it.
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(interval);
                function.run();
            }
        } catch (InterruptedException e) {
            // Wake up and exit thread
        }
    }

    /**
     * Used by the ExecutorService to create new threads
     * @param r the given Runnable
     * @return the created Thread
     */
    private Thread threadFactory(Runnable r) {
        Thread thread = new Thread(r, name);
        currentThread = thread;
        return thread;
    }
}
