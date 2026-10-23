package in.urlbit.common.error;

public class AliasTakenException extends RuntimeException {
    public AliasTakenException(String alias) {
        super("Alias '" + alias + "' is already in use");
    }
}
