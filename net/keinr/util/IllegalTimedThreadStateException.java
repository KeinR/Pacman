package net.keinr.util;

/**
 * Thrown to indicate that a TimedThread object is not ready for an operation
 * @author Orion Musselman (KeinR)
 * @version 1.0.0
 */

public class IllegalTimedThreadStateException extends IllegalThreadStateException {
    IllegalTimedThreadStateException(String message) {
        super(message);
    }
}
