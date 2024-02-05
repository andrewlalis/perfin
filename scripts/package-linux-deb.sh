#!/usr/bin/env bash

# Creates a native linux installer for Perfin, in the .deb format (for Ubuntu, Debian, etc.)

./mvnw clean package

function join_by {
   local d=${1-} f=${2-}
   if shift 2; then
     printf %s "$f" "${@/#/$d}"
   fi
}

# Fix because H2 is not modular:
rm target/lib/h2-*.jar

# Gets a ":"-separated string of all the dependency jar-files.
module_jar_files=(target/lib/*)
module_jar_files_path=$(join_by ":" ${module_jar_files[@]})
module_path="target/classes:$module_jar_files_path"

# Fix because H2 is not modular:
module_path="$module_path:target/modules/h2-2.2.224.jar"

jpackage \
  --name "Perfin" \
  --app-version "1.6.0" \
  --description "Desktop application for personal finance. Add your accounts, track transactions, and store receipts, invoices, and more." \
  --icon design/perfin-logo_256.png \
  --vendor "Andrew Lalis" \
  --about-url https://github.com/andrewlalis/perfin \
  --module com.andrewlalis.perfin/com.andrewlalis.perfin.PerfinApp \
  --module-path $module_path \
  --add-modules com.h2database \
  --type deb \
  --linux-deb-maintainer "andrewlalisofficial@gmail.com" \
  --linux-shortcut \
  --linux-menu-group "Office;Finance;Java" \
