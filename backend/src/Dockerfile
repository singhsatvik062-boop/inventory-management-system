FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN javac -cp "lib/mysql-connector-j-26.7.0.jar" Main.java

EXPOSE 8080

CMD ["java","-cp",".:lib/mysql-connector-j-26.7.0.jar","Main"]
