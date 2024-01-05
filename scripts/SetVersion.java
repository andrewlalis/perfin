import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Utility script that updates the program's version in the many places where
 * it's used, as opposed to making a fancy system to introspect the POM to get
 * the version. Simply call <code>java scripts/SetVersion.java &lt;your version here&gt;</code>
 * and it'll update all the necessary files.
 */
class SetVersion {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Missing required version argument.");
            System.exit(1);
        }
        String version = args[0].strip();
        System.out.println("Setting application to version " + version + " Is this okay? (yes/no)");
        var reader = new BufferedReader(new InputStreamReader(System.in));
        String response = reader.readLine();
        if (!response.equalsIgnoreCase("yes")) {
            System.out.println("Exiting.");
            System.exit(1);
        }

        replaceInFile(
            Path.of("scripts", "package-linux-deb.sh"),
            "--app-version .* \\\\",
            "--app-version \"" + version + "\" \\\\"
        );
        replaceInFile(
            Path.of("scripts", "package-windows-msi.ps1"),
            "--app-version .* `",
            "--app-version \"" + version + "\" `"
        );
        replaceInFile(
            Path.of("pom.xml"),
            "<version>.*</version>",
            "<version>" + version + "</version>"
        );
        replaceInFile(
            Path.of("src", "main", "resources", "main-view.fxml"),
            "text=\"Perfin .*\"",
            "text=\"Perfin Version " + version + "\""
        );
    }

    private static void replaceInFile(Path file, String pattern, String replacement) throws IOException {
        System.out.println("Replacing " + pattern + " with " + replacement + " in " + file);
        String fileContent = Files.readString(file);
        Files.writeString(file, fileContent.replaceFirst(pattern, replacement));
    }
}