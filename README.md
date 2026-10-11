# Fleet Rental Management System (FRMS)

ITS1033 Object-Oriented Programming - HDSE 77 Coursework
Author: <YOUR NAME / STUDENT ID>

A console-based Java application for managing a vehicle rental fleet
(Cars, Vans, Trucks), customers (Individual, Corporate) and rental agreements.
All code is in the single package `lk.ijse.fleet`.

## Requirements
- JDK 25 or later (the code uses no newer-than-Java-17 features, so it also compiles on earlier JDKs)

## Build and run
From the project root:

    javac -d bin $(find src -name "*.java")
    java -cp bin lk.ijse.fleet.Main

(Windows PowerShell: `javac -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName`)

## Features
- User registration and login (plain-text passwords, as required by the brief)
- Vehicles: Car, Van, Truck with polymorphic `calculateRentalCost`
- Customers: Individual (loyalty discount) and Corporate (15% discount + credit check)
- Custom checked exceptions: `VehicleNotAvailableException`, `InsufficientCreditException`
- `RentalEventListener` functional interface implemented with a lambda in `Main`
- Daemon threads: `VehicleStatusMonitor` and `OverdueReminderWorker`
  (every 5 seconds; background output may appear between menu prompts)
- `FleetManager.FleetSnapshot` non-static inner class
- Streams: available vehicles, vehicles by fuel type, sorted customers, revenue report
- `Reportable` interface implemented directly by `Vehicle` and `Customer`

## Quick demo
1. Register a user and log in
2. Add a vehicle (option 1) and a customer (option 2)
3. Create a rental agreement (option 4) using the generated IDs (VEH-1001, CUS-1001)
4. Try renting the same vehicle again to see the exception message
5. View reports (options 6, 7, 8) and return the vehicle (option 5)

## AI assistance disclosure
<EDIT THIS BEFORE SUBMITTING - be specific and honest.>
Example: "I used Claude (Anthropic) to help generate a first draft of the FRMS code
from the assignment brief and to test it. I reviewed and studied the code and can
explain it, and I take responsibility for the submitted work."
