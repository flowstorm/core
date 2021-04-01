#!/bin/bash
cd "$(dirname "$0")"
# app
cd app
yarn install
yarn build

# ui
cd ../lib/ui
yarn
yarn build
cp -r ./dist ../../app/dist/ui

# service
cd ../../lib/service
yarn
CI=false yarn build
cp ./build/bot-service.js ../../app/dist/service.js
