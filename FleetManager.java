package lk.ijse.fleet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FleetManager {
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<Customer> customers = new ArrayList<>();
    private List<RentalAgreement> agreements = new ArrayList<>();
    private RentalEventListener rentalEventListener; // association, may be null

    // Methods that touch the shared lists are synchronized because the two
    // daemon threads read/modify the same data while the menu is running.

    public synchronized void addVehicle(Vehicle v) {
        vehicles.add(v);
    }

    public synchronized void addCustomer(Customer c) {
        customers.add(c);
    }

    public synchronized Vehicle findVehicleById(String id) {
        for (Vehicle v : vehicles) {
            if (v.getVehicleId().equalsIgnoreCase(id)) {
                return v;
            }
        }
        return null;
    }

    public synchronized Customer findCustomerById(String id) {
        for (Customer c : customers) {
            if (c.getCustomerId().equalsIgnoreCase(id)) {
                return c;
            }
        }
        return null;
    }

    public void setRentalEventListener(RentalEventListener listener) {
        this.rentalEventListener = listener;
    }

    // Mission 4 version: Stream pipeline
    public synchronized List<Vehicle> getAvailableVehicles() {
        return vehicles.stream()
                .filter(v -> v.getStatus() == RentalStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    public synchronized RentalAgreement createRentalAgreement(String vehicleId, String customerId, int rentalDays)
            throws VehicleNotAvailableException, InsufficientCreditException {

        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle == null) {
            System.out.println("Vehicle not found: " + vehicleId);
            return null;
        }
        if (vehicle.getStatus() != RentalStatus.AVAILABLE) {
            throw new VehicleNotAvailableException("Vehicle " + vehicleId + " is not available (status: "
                    + vehicle.getStatus() + ").");
        }

        Customer customer = findCustomerById(customerId);
        if (customer == null) {
            System.out.println("Customer not found: " + customerId);
            return null;
        }

        double subtotal = vehicle.calculateRentalCost(rentalDays);
        double total = customer.applyDiscount(subtotal);

        if (customer instanceof CorporateCustomer) {
            CorporateCustomer corp = (CorporateCustomer) customer;
            if (!corp.hasSufficientCredit(total)) {
                throw new InsufficientCreditException(String.format(
                        "Customer %s does not have enough credit. Cost: %,.2f, Credit limit: %,.2f, Outstanding: %,.2f",
                        customerId, total, corp.getCreditLimit(), corp.getOutstandingBalance()));
            }
        }

        RentalAgreement agreement = new RentalAgreement(vehicle, customer, rentalDays);
        vehicle.setStatus(RentalStatus.RENTED);
        agreements.add(agreement);

        if (rentalEventListener != null) {
            rentalEventListener.onRentalCreated(agreement);
        }
        return agreement;
    }

    public synchronized void returnVehicle(String agreementId) {
        for (RentalAgreement a : agreements) {
            if (a.getAgreementId().equalsIgnoreCase(agreementId)) {
                a.getVehicle().setStatus(RentalStatus.AVAILABLE);
                System.out.println("Vehicle " + a.getVehicle().getVehicleId()
                        + " returned. Agreement " + agreementId + " closed.");
                return;
            }
        }
        System.out.println("Agreement not found: " + agreementId);
    }

    public synchronized List<Customer> getAllCustomers() {
        return new ArrayList<>(customers);
    }

    public synchronized List<RentalAgreement> getAllAgreements() {
        return new ArrayList<>(agreements);
    }

    // ---------------- Mission 3 ----------------

    public FleetSnapshot generateSnapshot() {
        return new FleetSnapshot();
    }

    public synchronized void printFleetStatus() {
        generateSnapshot().printSummary();
    }

    public synchronized void checkOverdueAgreements() {
        for (RentalAgreement a : agreements) {
            a.setDaysElapsed(a.getDaysElapsed() + 1);
            if (a.getDaysElapsed() > a.getRentalDays()) {
                System.out.println("[OVERDUE ALERT] Agreement " + a.getAgreementId()
                        + " has exceeded its " + a.getRentalDays() + "-day rental period.");
            }
        }
    }

    // ---------------- Mission 4 ----------------

    public synchronized List<Vehicle> getVehiclesByFuelType(FuelType type) {
        return vehicles.stream()
                .filter(v -> v.getFuelType() == type)
                .collect(Collectors.toList());
    }

    public synchronized List<Customer> getCustomersSortedByName() {
        return customers.stream()
                .sorted(Comparator.comparing(c -> c.getFullName()))
                .collect(Collectors.toList());
    }

    public synchronized double generateRevenueReport() {
        return agreements.stream()
                .mapToDouble(a -> a.getTotalCost())
                .sum();
    }

    public synchronized void printAllReportables() {
        List<Reportable> reportables = new ArrayList<>();
        reportables.addAll(vehicles);
        reportables.addAll(customers);
        if (reportables.isEmpty()) {
            System.out.println("Nothing to report yet.");
            return;
        }
        for (Reportable item : reportables) {
            System.out.println(item.getReportCategory() + ": " + item.getSummary());
        }
    }

    /**
     * Non-static inner class: it holds an implicit reference to the FleetManager
     * that created it, so it can read vehicles/agreements directly.
     */
    public class FleetSnapshot {
        private int availableCount;
        private int rentedCount;
        private double totalRevenue;

        public FleetSnapshot() {
            for (Vehicle v : vehicles) {
                if (v.getStatus() == RentalStatus.AVAILABLE) {
                    availableCount++;
                } else {
                    rentedCount++;
                }
            }
            for (RentalAgreement a : agreements) {
                totalRevenue += a.getTotalCost();
            }
        }

        public void printSummary() {
            System.out.println(String.format(
                    "[FLEET STATUS] Available: %d  Rented: %d  Total Revenue So Far: %,.2f",
                    availableCount, rentedCount, totalRevenue));
        }
    }
}
