# GridX - Intelligent Energy Marketplace & Smart Grid Simulator

A real-time Java desktop simulation for energy grid management with dynamic market pricing, battery storage, and failure scenarios.

## Screenshots

![GridX Dashboard](screenshot.png)

The dashboard shows a live view of the smart grid: city energy map with nodes (producers, consumers, batteries) connected by transmission lines, real-time generation/consumption/price metrics, market activity panel, failure injection controls, and an event log streaming simulation events.

## Features

- **Energy Producers**: Solar farms, wind farms, backup generators with real-time output simulation
- **Energy Consumers**: Homes, factories, offices, hospitals, EV charging stations with priority-based allocation
- **Smart Battery Management**: Charge/discharge optimization with efficiency losses and reserve maintenance
- **Energy Marketplace**: Real-time auction-based trading with dynamic pricing
- **Grid Simulation**: Transmission line congestion, energy losses, load balancing
- **Failure Scenarios**: Heat waves, power plant failures, blackouts, grid attacks
- **Real-time Dashboard**: Live visualization of grid state, market activity, and metrics
- **Event-Driven Architecture**: Comprehensive event logging and monitoring

## Tech Stack

- Java 17+
- Swing/AWT for GUI
- Java Collections Framework (ConcurrentHashMap, CopyOnWriteArrayList, PriorityBlockingQueue)
- Java Multithreading (ExecutorService, ScheduledExecutorService)

## How to Run

### Option 1: Using build script
```bash
./build.sh
```

### Option 2: Manual compilation
```bash
mkdir -p out
find src/main/java -name "*.java" | xargs javac -d out -sourcepath src/main/java
java -cp out com.gridx.ui.DashboardFrame
```

## Controls

- **Start/Pause/Resume**: Control simulation execution
- **Speed Slider**: Adjust simulation speed (1x-10x)
- **Failure Injection Panel**: Trigger various grid scenarios
- **Click nodes/lines**: View detailed information

## Simulation Scenarios

1. **Heat Wave**: Increases home demand by 2x
2. **Power Plant Failure**: Randomly disables a generator
3. **Wind Drop**: Reduces wind farm output
4. **Cloud Cover**: Reduces solar output
5. **Battery Failure**: Disables a battery
6. **Grid Attack**: Multiple random failures
7. **Blackout**: System-wide power failure
8. **Recovery**: Restore all failed components

## Architecture

```
com.gridx/
├── model/      # Energy nodes, lines, consumers, producers
├── engine/     # Energy balancer, pricing, failure injection
├── market/     # Order matching, auction system
├── event/      # Event bus, event types
├── simulation/ # Grid builder, simulation engine
├── metrics/    # Statistics tracking
└── ui/         # Dashboard, grid visualization
```

## License

MIT
