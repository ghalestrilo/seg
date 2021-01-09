#!/usr/bin/bash
# This is a very dumb but necessary script right now
# Because blessed (the JS TUI library) relies on pty.js (hich in turn is deprecated')
# So the """fix""" is to install node-pty (the officially supported lib) and trick node
# Into loading the more recent one

npm install node-pty
cd node_modules
ln -s node-pty pty.js
cd ..
