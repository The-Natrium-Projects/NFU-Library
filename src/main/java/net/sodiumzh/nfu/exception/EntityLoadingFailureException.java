package net.sodiumzh.nfu.exception;

public class EntityLoadingFailureException extends RuntimeException {

    public EntityLoadingFailureException(String msg) {
        super(msg);
    }

    public EntityLoadingFailureException(Throwable cause) {
        super(cause);
    }

    public EntityLoadingFailureException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
