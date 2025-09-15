#!/bin/bash

# Path of executable script

EXECUTABLE ="CollectCDCStats.sh"

# Delay in seconds between each run

DELAY =5

cleanup() {

	echo ""
	echo "Interrupted by user (Ctrl+C). Exiting gracefully..."
	exit 0
}

# Trap SIGINT (Ctrl+C) and call cleanup()
trap cleanup SIGINT

# Infinite loop
while true; do 
	echo "Running: ${EXECUTABLE}"
	./${EXECUTABLE}
   
	if [ $? -ne 0 ]; then 
		echo "Executable exited with an error."
		break
	fi
   
	if [ -f "*.STOPPED*" ]; then
		echo "Stop file found. Exiting loop."
		break
	fi
   
   
	echo "Sleeping for ${DELAY} seconds..."
	sleep ${DELAY}
   
done
