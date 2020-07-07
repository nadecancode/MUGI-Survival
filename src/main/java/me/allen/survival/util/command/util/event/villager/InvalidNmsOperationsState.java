package me.allen.survival.util.command.util.event.villager;

public class InvalidNmsOperationsState extends RuntimeException {
    public InvalidNmsOperationsState(final String message) {
        super(message);
    }
    public InvalidNmsOperationsState(final String message, Throwable cause) {
        super(message, cause);
    }
}