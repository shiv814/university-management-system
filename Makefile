SOURCES := $(shell find src/main/java -name '*.java')
TESTS := $(shell find src/test/java -name '*.java')
JAVAC_FLAGS := -Xlint:all -Werror

.PHONY: build test run demo clean

build:
	mkdir -p build/classes
	javac $(JAVAC_FLAGS) -d build/classes $(SOURCES)

test: build
	javac $(JAVAC_FLAGS) -cp build/classes -d build/classes $(TESTS)
	java -ea -cp build/classes ca.shivam.university.TestRunner

run: build
	java -cp build/classes ca.shivam.university.Main

demo: build
	printf '%s\n' 'student 1001 "Shivam Patel" shivam@example.com "Computer Engineering" 2' 'course CIS1500 "Introduction to Programming" 2 0.5 Fall' 'enroll 1001 CIS1500' 'complete 1001 CIS1500 88' 'transcript 1001' 'dashboard' 'quit' | java -cp build/classes ca.shivam.university.Main

clean:
	rm -rf build
