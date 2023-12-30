#!/usr/bin/env bash

shouldBuild=0
for i in "$@" ; do
  if [[ $i == "build" ]] ; then
    shouldBuild=1
    break
  fi
done

if [ $shouldBuild == 1 ]; then
  mvn clean
  mvn package
fi

java \
  --add-modules=javafx.controls,com.andrewlalis.javafx_scene_router \
  --module-path=target/lib/ \
  -jar target/perfin-*-jar-with-dependencies.jar
