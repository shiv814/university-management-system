SOURCES := $(shell find src/main/java -name '*.java')
TESTS := $(shell find src/test/java -name '*.java')

build:
	mkdir -p build/classes
	javac -d build/classes $(SOURCES)

test: build
	javac -cp build/classes -d build/classes $(TESTS)
	java -cp build/classes ca.shivam.university.TestRunner

run: build
	java -cp build/classes ca.shivam.university.Main

clean:
	rm -rf build
