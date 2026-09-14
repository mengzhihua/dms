package com.dms.oms.client;

public class OmsException extends RuntimeException {
    public OmsException(String message) {
        super(message);
    }

    public OmsException(String message, Throwable cause) {
        super(message, cause);
    }
}
