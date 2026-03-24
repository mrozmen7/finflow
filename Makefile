.PHONY: run test build clean docker-up docker-down

run:
	./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

test:
	./mvnw test

test-integration:
	./mvnw test -Pintegration

build:
	./mvnw clean package -DskipTests

clean:
	./mvnw clean

docker-up:
	docker compose -f infra/docker/docker-compose.yml up -d

docker-down:
	docker compose -f infra/docker/docker-compose.yml down

lint:
	./mvnw checkstyle:check

coverage:
	./mvnw test jacoco:report
	@echo 'Coverage report: target/site/jacoco/index.html'
