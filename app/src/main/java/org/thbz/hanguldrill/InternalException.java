package org.thbz.hanguldrill;

/**
 * Created by Thierry on 20/12/14.
 */
public class InternalException extends Exception {
    InternalException(String message) {
        super(message);
    }

    InternalException(Exception exc) {
        this(exc.getMessage());
    }
}
