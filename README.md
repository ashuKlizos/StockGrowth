# StockGrowth Project

This project consists of a Spring Boot backend and a React frontend for stock analysis and tracking.

## Prerequisites

- Java JDK 17 or higher
- Node.js (v16 or higher)
- npm (comes with Node.js)
- Maven
- Git
- MySQL 8.0 or higher

## Project Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd StockGrowth
```

### 2. Database Setup

1. Install MySQL if not already installed
2. Create a new database:
```sql
CREATE DATABASE stock_growth;
```
3. Update database configuration in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/stock_growth
spring.datasource.username=<your-mysql-username>
spring.datasource.password=<your-mysql-password>
```

### 3. Backend Setup (Spring Boot)

1. Navigate to the root directory:
```bash
cd StockGrowth
```

2. Build the project using Maven:
```bash
./mvnw clean install
```
For Windows users:
```bash
mvnw.cmd clean install
```

3. Run the Spring Boot application:
```bash
./mvnw spring-boot:run
```
For Windows users:
```bash
mvnw.cmd spring-boot:run
```

The backend server will start on `http://localhost:8080`

### 4. Frontend Setup (React)

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm start
```

The frontend application will start on `http://localhost:3000`

## Project Structure

```
StockGrowth/
├── frontend/               # React frontend
│   ├── src/
│   │   ├── components/    # React components
│   │   ├── services/      # API services
│   │   └── types/         # TypeScript type definitions
│   └── package.json
│
└── src/                   # Spring Boot backend
    └── main/
        └── java/
            └── com/
                └── StockGrowth/
                    ├── config/
                    ├── controller/
                    ├── dto/
                    ├── model/
                    ├── repository/
                    └── service/
```

## Available Scripts

In the frontend directory:
- `npm start`: Runs the frontend in development mode
- `npm test`: Launches the test runner
- `npm run build`: Builds the frontend for production

In the root directory:
- `./mvnw test`: Runs backend tests
- `./mvnw spring-boot:run`: Starts the backend server
- `./mvnw clean install`: Builds the backend

## Configuration

### Backend
The backend configuration is in `src/main/resources/application.properties`. Key settings include:

```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/stock_growth
spring.datasource.username=<your-mysql-username>
spring.datasource.password=<your-mysql-password>
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update

# Logging Configuration
logging.level.org.springframework.security=DEBUG
logging.level.com.StockGrowth=DEBUG
logging.level.org.springframework.web=DEBUG
```

### Frontend
- Environment variables can be set in `.env` file in the frontend directory

## Additional Notes

1. Make sure both backend and frontend servers are running simultaneously for full functionality
2. The backend API must be running on port 8080
3. The frontend development server runs on port 3000 by default
4. The application uses MySQL for data persistence. Make sure MySQL server is running before starting the backend
5. The database schema will be automatically created/updated by Hibernate (spring.jpa.hibernate.ddl-auto=update) 