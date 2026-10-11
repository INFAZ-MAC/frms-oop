package lk.ijse.fleet;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    private FleetManager fleetManager;
    private UserManager userManager;
    private Scanner scanner;
    private SystemUser loggedInUser; // null until login succeeds

    private static final String LINE =
            "========================================================";

    public Main() {
        fleetManager = new FleetManager();
        userManager = new UserManager();
        scanner = new Scanner(System.in);

        // Lambda implementing the RentalEventListener functional interface
        fleetManager.setRentalEventListener(agreement ->
                System.out.println("[EVENT] New rental created: " + agreement.getAgreementId())
        );

        // Background workers as daemon threads
        Thread statusThread = new Thread(new VehicleStatusMonitor(fleetManager));
        statusThread.setDaemon(true);
        statusThread.start();

        Thread overdueThread = new Thread(new OverdueReminderWorker(fleetManager));
        overdueThread.setDaemon(true);
        overdueThread.start();
    }

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        while (true) {
            if (loggedInUser == null) {
                showAuthMenu();
            } else {
                showMainMenu();
            }
        }
    }

    // ------------------------------------------------------------------
    // Auth menu
    // ------------------------------------------------------------------
    public void showAuthMenu() {
        while (loggedInUser == null) {
            System.out.println(LINE);
            System.out.println("FLEET RENTAL MANAGEMENT SYSTEM (FRMS)");
            System.out.println(LINE);
            System.out.println("[1] Login");
            System.out.println("[2] Register New User");
            System.out.println("[0] Exit");
            int choice = readChoice();
            switch (choice) {
                case 1:
                    String username = readNonEmpty("Username > ");
                    String password = readNonEmpty("Password > ");
                    SystemUser user = userManager.login(username, password);
                    if (user != null) {
                        loggedInUser = user;
                    } else {
                        System.out.println("Invalid username or password");
                    }
                    break;
                case 2:
                    String newUsername = readNonEmpty("Choose username > ");
                    String newPassword = readNonEmpty("Choose password > ");
                    String fullName = readNonEmpty("Full name > ");
                    userManager.registerUser(newUsername, newPassword, fullName);
                    break;
                case 0:
                    System.out.println("Goodbye!");
                    System.exit(0);
                    break;
                default:
                    if (choice != -1) {
                        System.out.println("Invalid option. Please try again.");
                    }
            }
        }
    }

    // ------------------------------------------------------------------
    // Main menu
    // ------------------------------------------------------------------
    public void showMainMenu() {
        System.out.println("Welcome, " + loggedInUser.getFullName() + "! Loading fleet dashboard...");
        while (loggedInUser != null) {
            System.out.println(LINE);
            System.out.println("FRMS MAIN MENU | Logged in as: " + loggedInUser.getFullName());
            System.out.println(LINE);
            System.out.println("[1] Add Vehicle");
            System.out.println("[2] Add Customer");
            System.out.println("[3] View Available Vehicles");
            System.out.println("[4] Create Rental Agreement");
            System.out.println("[5] Return Vehicle");
            System.out.println("[6] View Revenue Report");
            System.out.println("[7] View Vehicles by Fuel Type");
            System.out.println("[8] View Combined Fleet & Customer Report");
            System.out.println("[9] View All Customers");
            System.out.println("[10] View All Rental Agreements");
            System.out.println("[0] Logout");
            int choice = readChoice();
            switch (choice) {
                case 1: addVehicle(); break;
                case 2: addCustomer(); break;
                case 3: viewAvailableVehicles(); break;
                case 4: createRental(); break;
                case 5: returnVehicle(); break;
                case 6: viewRevenueReport(); break;
                case 7: viewVehiclesByFuelType(); break;
                case 8: fleetManager.printAllReportables(); break;
                case 9: viewAllCustomers(); break;
                case 10: viewAllAgreements(); break;
                case 0:
                    loggedInUser = null;
                    System.out.println("Logged out.");
                    break;
                default:
                    if (choice != -1) {
                        System.out.println("Invalid option. Please try again.");
                    }
            }
        }
    }

    // ------------------------------------------------------------------
    // Menu actions
    // ------------------------------------------------------------------
    private void addVehicle() {
        System.out.println("Vehicle type: [1] Car  [2] Van  [3] Truck");
        int type = readIntLoop("Type > ", 1, 3);
        String reg = readNonEmpty("Registration number > ");
        String brand = readNonEmpty("Brand > ");
        String model = readNonEmpty("Model > ");
        double rate = readPositiveDouble("Daily rate > ");
        FuelType fuel = readFuelType();

        Vehicle vehicle;
        if (type == 1) {
            vehicle = new Car(reg, brand, model, rate, fuel);
        } else if (type == 2) {
            int capacity = readIntLoop("Passenger capacity > ", 1, 100);
            vehicle = new Van(reg, brand, model, rate, fuel, capacity);
        } else {
            double tons = readPositiveDouble("Cargo capacity (tons) > ");
            vehicle = new Truck(reg, brand, model, rate, fuel, tons);
        }
        fleetManager.addVehicle(vehicle);
        System.out.println("Vehicle added: " + vehicle.getVehicleId());
    }

    private void addCustomer() {
        System.out.println("Customer type: [1] Individual  [2] Corporate");
        int type = readIntLoop("Type > ", 1, 2);
        String name = readNonEmpty("Full name > ");
        String contact = readNonEmpty("Contact number > ");

        Customer customer;
        if (type == 1) {
            int points = readIntLoop("Loyalty points > ", 0, Integer.MAX_VALUE);
            customer = new IndividualCustomer(name, contact, points);
        } else {
            double limit = readPositiveDouble("Credit limit > ");
            customer = new CorporateCustomer(name, contact, limit);
        }
        fleetManager.addCustomer(customer);
        System.out.println("Customer added: " + customer.getCustomerId());
    }

    private void viewAvailableVehicles() {
        List<Vehicle> available = fleetManager.getAvailableVehicles();
        if (available.isEmpty()) {
            System.out.println("No vehicles available.");
            return;
        }
        for (Vehicle v : available) {
            System.out.println(v);
        }
    }

    private void createRental() {
        String vehicleId = readNonEmpty("Vehicle ID (e.g. VEH-1001) > ");
        String customerId = readNonEmpty("Customer ID (e.g. CUS-1001) > ");
        int days = readIntLoop("Rental days > ", 1, 3650);
        try {
            RentalAgreement agreement = fleetManager.createRentalAgreement(vehicleId, customerId, days);
            if (agreement != null) {
                System.out.println(agreement);
            }
        } catch (VehicleNotAvailableException | InsufficientCreditException e) {
            System.out.println("Rental failed: " + e.getMessage());
        }
    }

    private void returnVehicle() {
        String agreementId = readNonEmpty("Agreement ID (e.g. AGR-1001) > ");
        fleetManager.returnVehicle(agreementId);
    }

    private void viewRevenueReport() {
        String dashes = "----------------------------------------------------------------";
        System.out.println("================================================================");
        System.out.println("FLEET REVENUE REPORT");
        System.out.println("================================================================");
        System.out.println(String.format("Total Revenue: %,.2f", fleetManager.generateRevenueReport()));
        System.out.println(dashes);
    }

    private void viewVehiclesByFuelType() {
        FuelType type = readFuelType();
        List<Vehicle> list = fleetManager.getVehiclesByFuelType(type);
        if (list.isEmpty()) {
            System.out.println("No " + type + " vehicles found.");
            return;
        }
        for (Vehicle v : list) {
            System.out.println(v);
        }
    }

    private void viewAllCustomers() {
        List<Customer> list = fleetManager.getAllCustomers();
        if (list.isEmpty()) {
            System.out.println("No customers registered.");
            return;
        }
        for (Customer c : list) {
            System.out.println(c);
        }
    }

    private void viewAllAgreements() {
        List<RentalAgreement> list = fleetManager.getAllAgreements();
        if (list.isEmpty()) {
            System.out.println("No rental agreements yet.");
            return;
        }
        for (RentalAgreement a : list) {
            System.out.println(a);
        }
    }

    // ------------------------------------------------------------------
    // Input helpers (validation: nothing here lets bad input crash the app)
    // ------------------------------------------------------------------
    private String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException e) { // input stream closed
            System.out.println();
            System.exit(0);
            return "";
        }
    }

    /** Menu choice: returns -1 (after a friendly message) if not a number. */
    private int readChoice() {
        String input = readLine("Enter choice > ");
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return -1;
        }
    }

    private int readIntLoop(String prompt, int min, int max) {
        while (true) {
            String input = readLine(prompt);
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private double readPositiveDouble(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                double value = Double.parseDouble(input);
                if (value > 0) {
                    return value;
                }
                System.out.println("Value must be greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private String readNonEmpty(String prompt) {
        while (true) {
            String input = readLine(prompt);
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    private FuelType readFuelType() {
        FuelType[] types = FuelType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.println("[" + (i + 1) + "] " + types[i]);
        }
        int choice = readIntLoop("Fuel type > ", 1, types.length);
        return types[choice - 1];
    }
}
