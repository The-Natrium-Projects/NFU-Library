package net.sodiumzh.nfu.exception;

/**
 * Thrown if some registration is required for some operation but the registration is missing.
 */
public class MissingRegistryEntryException extends RuntimeException {

    public MissingRegistryEntryException(String desc) {
        super(desc);
    }

    public MissingRegistryEntryException(String desc, Exception reason) {
        super(desc, reason);
    }

}
