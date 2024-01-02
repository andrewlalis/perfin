$projectDir = $PSScriptRoot

./mvnw.cmd clean package

$modules = Get-ChildItem -Path target/lib -Name | ForEach-Object { "lib\$_" }
