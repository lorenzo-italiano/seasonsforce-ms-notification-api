mvn clean install

mv target/seasonsforce-ms-notification-api-1.0-SNAPSHOT.jar api-image/seasonsforce-ms-notification-api-1.0-SNAPSHOT.jar

cd api-image

docker build -t notification-api .

cd ../mongo-image

docker build -t notification-db .
