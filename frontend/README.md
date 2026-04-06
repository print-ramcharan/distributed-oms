# Zentra Control Center (Frontend)

The Zentra Control Center is a real-time dashboard built with React and Vite for monitoring and managing the Distributed Order Management System.

## Features

- **Real-time Monitoring**: Visualizes JVM metrics, Kafka consumer lag, and system health.
- **Chaos Hub**: Interface for injecting faults (latency, service kills) into the backend.
- **DLQ Management**: View and replay failed Kafka messages from the Dead Letter Queue.
- **Order Simulator**: Place various order scenarios to test the Saga orchestration.

## Tech Stack

- **Framework**: React 18
- **Build Tool**: Vite
- **Styling**: TailwindCSS v4
- **State Management**: Zustand
- **Charts**: Recharts
- **Icons**: Lucide React

## Getting Started

### Prerequisites

- Node.js 18+
- npm 9+

### Installation

1.  Navigate to the frontend directory:
    ```bash
    cd frontend
    ```
2.  Install dependencies:
    ```bash
    npm install
    ```

### Development

Start the development server:
```bash
npm run dev
```
The application will be available at `http://localhost:5173`.

## Environment Configuration

The frontend communicates with the backend services through the API Gateway (default: `http://localhost:8080`). Configuration for the dev proxy can be found in `vite.config.js`.
