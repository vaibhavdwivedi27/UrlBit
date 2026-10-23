package in.urlbit.common.error;

public class GuestQuotaExceededException extends RuntimeException {
    public GuestQuotaExceededException() {
        super("Guest creation quota of 5 short links has been reached");
    }
}
