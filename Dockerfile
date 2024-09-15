# Этап 1: Сборка JAR-файла
FROM maven:3.8.5-openjdk-17 AS build

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем файл pom.xml и загружаем зависимости
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходный код проекта
COPY src ./src

# Собираем JAR-файл
RUN mvn package -DskipTests

# Этап 2: Создание финального образа
FROM openjdk:17-slim

# Установка необходимых пакетов и добавление репозитория Google Chrome
RUN apt-get update && \
    apt-get install -y gnupg wget curl unzip --no-install-recommends && \
    wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list && \
    apt-get update -y

# Установка Google Chrome
RUN apt-get install -y google-chrome-stable

# Установка ChromeDriver
ENV CHROMEDRIVER_VERSION=114.0.5735.90
RUN wget -q --continue -P /chromedriver "https://chromedriver.storage.googleapis.com/$CHROMEDRIVER_VERSION/chromedriver_linux64.zip" && \
    unzip /chromedriver/chromedriver_linux64.zip -d /usr/local/bin/ && \
    rm -rf /chromedriver

# Установка LibreOffice
RUN apt-get update && apt-get install -y \
    libreoffice \
    libreoffice-writer \
    libreoffice-calc \
    libreoffice-impress \
    procps && \
    rm -rf /var/lib/apt/lists/*

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR-файл из предыдущего этапа
COPY --from=build /app/target/*.jar app.jar

# Копируем скрипт entrypoint
COPY entrypoint.sh entrypoint.sh

RUN chmod +x entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]
