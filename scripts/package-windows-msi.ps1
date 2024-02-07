./mvnw.cmd clean package

# Fix because H2 is not modular:
Remove-Item -Path target\lib\h2-*.jar -Force

$modules = Get-ChildItem -Path target\lib -Name | ForEach-Object { "target\lib\$_" }
$modulePath = $modules -join ';'
$modulePath = "target\classes;$modulePath"

# Fix because H2 is not modular:
$modulePath = "$modulePath;target\modules\h2-2.2.224.jar"

jpackage `
  --name "Perfin" `
  --app-version "1.7.1" `
  --description "Desktop application for personal finance. Add your accounts, track transactions, and store receipts, invoices, and more." `
  --icon design\perfin-logo_256.ico `
  --vendor "Andrew Lalis" `
  --about-url https://github.com/andrewlalis/perfin `
  --module com.andrewlalis.perfin/com.andrewlalis.perfin.PerfinApp `
  --module-path $modulePath `
  --add-modules com.h2database `
  --type msi `
  --win-menu `
  --win-shortcut-prompt `
  --win-help-url https://github.com/andrewlalis/perfin `
