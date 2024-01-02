#!/usr/bin/env bash

# A helper script to (optionally) build and run a JAR packaged version of Perfin.
# Provide the "build" argument to rebuild the project before running it.

shouldBuild=0
for i in "$@" ; do
  if [[ $i == "build" ]] ; then
    shouldBuild=1
    break
  fi
done

if [ $shouldBuild == 1 ]; then
  ./mvnw clean package
fi

java \
  --add-modules=javafx.controls,com.andrewlalis.javafx_scene_router \
  --module-path=target/lib/ \
  -jar target/perfin-*-jar-with-dependencies.jar
