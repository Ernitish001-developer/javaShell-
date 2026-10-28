import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class MyShell {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        Path currentDirectory = Paths.get("").toAbsolutePath();

        List<String> history = new ArrayList<>();

        while (true) {

            System.out.print("myshell> ");

            String input = sc.nextLine().trim();

            // Ignore empty input
            if (input.isEmpty()) {
                continue;
            }

            // Add command to history
            history.add(input);

            // Exit command
            if (input.equals("exit")) {
                System.out.println("Shell closed.");
                break;
            }

            // Split command and arguments
            String[] parts = input.split("\\s+", 2);
            String command = parts[0];

            String argument = "";

            if (parts.length > 1) {
                argument = parts[1].trim();
            }

            try {

                // 1. ECHO COMMAND
                if (command.equals("echo")) {

                    System.out.println(argument);

                }

                // 2. PWD COMMAND
                else if (command.equals("pwd")) {

                    System.out.println(currentDirectory);

                }

                // 3. DIR COMMAND
                else if (command.equals("dir")) {

                    ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "dir");
                    pb.directory(currentDirectory.toFile());
                    pb.inheritIO();

                    Process process = pb.start();
                    process.waitFor();

                }

                // 4. TYPE COMMAND
                else if (command.equals("type")) {

                    if (argument.isEmpty()) {
                        System.out.println("Usage: type <command>");
                    } else {

                        String[] builtins = {
                            "echo", "pwd", "dir", "cd",
                            "mkdir", "clear", "help",
                            "history", "type", "exit"
                        };

                        if (Arrays.asList(builtins).contains(argument)) {
                            System.out.println(argument + " is a shell builtin");
                        } else {
                            System.out.println(argument + " is an external command");
                        }
                    }

                }

                // 5. PING COMMAND
                else if (command.equals("ping")) {

                    if (argument.isEmpty()) {
                        System.out.println("Usage: ping <options> <host>");
                    } else {

                        ProcessBuilder pb = new ProcessBuilder(
                            "cmd.exe", "/c", "ping " + argument
                        );

                        pb.directory(currentDirectory.toFile());
                        pb.inheritIO();

                        Process process = pb.start();
                        process.waitFor();
                    }

                }

                // 6. CD COMMAND
                else if (command.equals("cd")) {

                    if (argument.isEmpty()) {

                        System.out.println(currentDirectory);

                    } else {

                        Path newDirectory = currentDirectory.resolve(argument)
                                .normalize()
                                .toAbsolutePath();

                        if (Files.isDirectory(newDirectory)) {

                            currentDirectory = newDirectory;

                        } else {

                            System.out.println("Error: Directory not found.");

                        }
                    }

                }

                // 7. MKDIR COMMAND
                else if (command.equals("mkdir")) {

                    if (argument.isEmpty()) {

                        System.out.println("Usage: mkdir <foldername>");

                    } else {

                        Path newDirectory = currentDirectory.resolve(argument)
                                .normalize();

                        Files.createDirectories(newDirectory);

                        System.out.println("Directory created: " + newDirectory);
                    }

                }

                // 8. CLEAR COMMAND
                else if (command.equals("clear")) {

                    for (int i = 0; i < 40; i++) {
                        System.out.println();
                    }

                }

                // 9. HELP COMMAND
                else if (command.equals("help")) {

                    System.out.println("\nAvailable MyShell Commands:");
                    System.out.println("echo <text>        - Print a message");
                    System.out.println("pwd                - Show current directory");
                    System.out.println("dir                - List files and folders");
                    System.out.println("type <command>     - Show command type");
                    System.out.println("ping <host>        - Test network connection");
                    System.out.println("cd <directory>     - Change directory");
                    System.out.println("mkdir <foldername> - Create a directory");
                    System.out.println("clear              - Clear shell output");
                    System.out.println("help               - Show available commands");
                    System.out.println("history            - Show command history");
                    System.out.println("exit               - Close MyShell\n");

                }

                // 10. HISTORY COMMAND
                else if (command.equals("history")) {

                    System.out.println("\nCommand History:");

                    for (int i = 0; i < history.size(); i++) {
                        System.out.println((i + 1) + ". " + history.get(i));
                    }

                    System.out.println();

                }

                // 11. UNKNOWN COMMAND / EXTERNAL COMMAND
                else {

                    ProcessBuilder pb = new ProcessBuilder(
                        "cmd.exe", "/c", input
                    );

                    pb.directory(currentDirectory.toFile());
                    pb.inheritIO();

                    Process process = pb.start();
                    int exitCode = process.waitFor();

                    if (exitCode != 0) {
                        System.out.println(
                            "Command failed with exit code: " + exitCode
                        );
                    }
                }

            } catch (IOException e) {

                System.out.println("Error: " + e.getMessage());

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                System.out.println("Command execution interrupted.");

                break;

            } catch (Exception e) {

                System.out.println("Error: " + e.getMessage());

            }
        }

        sc.close();
    }
}