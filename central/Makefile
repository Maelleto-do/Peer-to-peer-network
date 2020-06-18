NAME = tracker
CFLAGS = -Wall -std=c99
PEER_PORT ?= 7000
TRACKER_PORT ?= $(shell (ls Config/config_tracker.ini 2> /dev/null > /dev/null && grep "tracker-port = " Config/config_tracker.ini  | sed 's/tracker-port = //') || echo 8080)

JAVA="ClientHandler.java" \
		"Command.java" \
		"RequestHave.java" \
		"RequestAnnounce.java" \
		"RequestGetfile.java" \
		"RequestGetpieces.java" \
		"RequestInterested.java" \
		"RequestLook.java" \
		"RequestUpdate.java" \
		"Peer.java"

all: build_folder compile

build_folder:
	mkdir -p build;

compile: Tracker Peer

configuration: write_config.sh
	./write_config.sh

Tracker: build_folder
	mkdir -p build/tracker;
	for File in `ls tracker | grep "\.c" | sed 's/\.c//'`; do \
		echo "$$File"; \
		gcc $(CFLAGS) tracker/$$File.c -c -o build/tracker/$$File.o -lpthread; \
	done
	gcc $(CFLAG) build/tracker/*.o -o build/tracker/tracker -lpthread

execTracker: Tracker
	./build/tracker/tracker ${TRACKER_PORT}

Peer: build_folder
	javac -d build -cp build peer/Command.java peer/ClientHandler.java;
	for class in $(JAVA); do \
		echo -ne "\t\t$$class"; \
	 	javac -d build -cp build peer/$$class; \
	done

execPeer: Peer
	java -ea -cp build Peer 8000

launchPeer:
	java -ea -cp build Peer 8000

rm logs:
	rm -f ./Logs/*

clean:
	rm -rf ./build/*
	rm -f config_*
