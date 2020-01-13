#!/bin/sh

# Explicitly define HOME otherwise it might not have been set
export HOME=/muwire

echo "Starting MuWire"
exec /muwire/bin/MuWire
