package lk.ijse.fleet;

/** Checked exception: thrown when a corporate customer lacks remaining credit. */
public class InsufficientCreditException extends Exception {
    private static final long serialVersionUID = 1L;

    public InsufficientCreditException(String message) {
        super(message);
    }
}
