
# SpringBoot File Conversion Service

## Overview

This SpringBoot application is designed to run inside a Docker container and is built using Java 17. It provides file conversion services, specifically converting Excel to PDF and HTML to PDF. The project is set up with two different configuration files to facilitate local runs and containerized deployments.

## Prerequisites

Before you start, make sure you have the following installed on your system:
- JDK 17
- Maven (for building the project)
- Docker (for containerization)

## Building the Project

To compile the project and generate a jar file, use Maven. Run the following command in the root directory of the project:

```bash
mvn clean package -DskipTests
```

This command compiles the project and produces a jar file located in the `target/` directory, skipping the execution of tests for faster building.

## Configuration Files

The project utilizes two configuration files:
- `application-local.properties` for running the application locally.
- `application-docker.properties` for running the application in a Docker container.

Ensure you have the correct settings in `application-docker.properties` for your Docker environment.

## Building and Running the Docker Container

After compiling the jar, you can create and run a Docker container with the application. Follow these steps:

1. **Build the Docker Image:**

    Create a `Dockerfile` in the project root with the necessary instructions to build your image. Then, run:

    ```bash
    docker build -t file-conversion-service .
    ```

2. **Run the Container:**

    Once the image is built, start a container:

    ```bash
    docker run -d -p 8080:8080 file-conversion-service
    ```

    Adjust the port mappings as necessary for your setup.

## Available Endpoints

The application exposes 4 endpoints for file conversion:

- **Excel to PDF Conversion:** `POST /api/converter/excel2pdf`

    Attach the Excel file you wish to convert in the request body.


- **HTML to PDF Conversion:** `POST /api/converter/html2pdf`

    Attach the HTML file you wish to convert in the request body.


- **JSON to Excel Conversion:** `POST /api/converter/json2excel`

    Attach the JSON file you wish to convert in the request body.


- **JSON to CSV Conversion:** `POST /api/converter/json2csv`

   Attach the JSON file you wish to convert in the request body.
## Usage Example

To use the conversion service, send a POST request to the relevant endpoint with the file attached. Here's an example using `curl`:

```bash
curl -X POST -F "file=@/path/to/your/file.xlsx" http://localhost:8080/api/converter/excel2pdf
```

Replace `/path/to/your/file.xlsx` with the path to the Excel file you want to convert and adjust the URL based on your setup.

## Conclusion

This guide should help you get the file conversion service up and running inside a Docker container. If you encounter any issues, make sure to check the configuration files and Docker settings.
