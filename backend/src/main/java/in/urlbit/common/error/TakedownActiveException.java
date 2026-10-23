package in.urlbit.common.error;

public class TakedownActiveException extends RuntimeException {
    public TakedownActiveException() {
        super("Link has been taken down for abuse and cannot be modified");
    }
}
