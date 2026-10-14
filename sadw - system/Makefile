export JAVA_HOME ?= $(shell /usr/libexec/java_home -v 17 2>/dev/null || echo $$JAVA_HOME)

.PHONY: check test build clean

check:
	./gradlew check

test:
	./gradlew test

build:
	./gradlew build

clean:
	./gradlew clean
