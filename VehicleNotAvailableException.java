package lk.ijse.fleet;

/** Checked exception: thrown when renting a vehicle that is not AVAILABLE. */
public class VehicleNotAvailableException extends Exception {
    private static final long serialVersionUID = 1L;

    public VehicleNotAvailableException(String message) {
        super(message);
    }
}
