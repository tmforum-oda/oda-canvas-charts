#!/bin/bash
cd ../portal-web
npm i && npm run build
cd ../build

# copy front output to static dir
#./copy-nodejs-dist-to-static.sh

cd ..

# build jar
mvn clean -e install -Dmaven.test.skip=true -U dependency:copy-dependencies -DoutputDirectory=./target/lib

cp ./build/Dockerfile ./portal-service/target/

cd ./portal-service/target

current_time=$(date '+%Y%m%d%H%M%S')

docker build . -t oda-dashboard:${current_time}

echo "canvas portal image oda-dashboard:${current_time} build success"