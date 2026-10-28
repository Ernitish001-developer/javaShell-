import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class ShellServer {

    private static final Path ROOT =
            Paths.get("workspace").toAbsolutePath().normalize();

    private static Path currentDir = ROOT;

    public static void main(String[] args) throws Exception {

        Files.createDirectories(ROOT);

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 8080), 0);

        server.createContext("/api/command", exchange -> {
            addCors(exchange);

            try {
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    send(exchange, 405, "POST requests only.");
                    return;
                }

                String command = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8).trim();

                String result = executeCommand(command);

                send(exchange, 200, result);

            } catch (Exception e) {
                send(exchange, 500, "Error: " + e.getMessage());
            } finally {
                exchange.close();
            }
        });

        server.setExecutor(null);
        server.start();

        System.out.println("JavaShell server started!");
        System.out.println("Workspace: " + ROOT);
        System.out.println("Server: http://127.0.0.1:8080");
    }

    private static void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "http://127.0.0.1:5500");
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers", "Content-Type");
    }

    private static void send(
            HttpExchange exchange, int status, String response)
            throws IOException {

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static String executeCommand(String input) throws IOException {

        if (input.isEmpty()) {
            return "Please enter a command.";
        }

        String[] parts = input.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {

            case "help":
                return """
                        Available Commands
                        ------------------
                        help             Show commands
                        echo <text>      Print text
                        pwd              Show current directory
                        dir              List files and folders
                        cd <folder>      Change directory
                        mkdir <folder>   Create a folder
                        create <file>    Create a text file
                        cat <file>       Read a text file
                        date             Show date and time
                        whoami           Show username
                        clear            Clear terminal
                        exit             Show exit message
                        """;

            case "echo":
                return arg;

            case "pwd":
                return currentDir.toString();

            case "dir":
                try (Stream<Path> paths = Files.list(currentDir)) {
                    StringBuilder output = new StringBuilder();

                    paths.sorted().forEach(path -> output.append(
                            Files.isDirectory(path) ? "[DIR]  " : "[FILE] ")
                            .append(path.getFileName())
                            .append("\n"));

                    return output.isEmpty()
                            ? "Directory is empty."
                            : output.toString();
                }

            case "cd":
                if (arg.isEmpty()) {
                    return "Usage: cd <folder>";
                }

                Path newDir = resolvePath(arg);

                if (!Files.exists(newDir) ||
                        !Files.isDirectory(newDir)) {
                    return "Error: Directory not found.";
                }

                currentDir = newDir;
                return "Directory changed to: " + currentDir;

            case "mkdir":
                if (!validName(arg)) {
                    return "Usage: mkdir <folder-name>";
                }

                Path folder = resolvePath(arg);

                if (Files.exists(folder)) {
                    return "Error: File or folder already exists.";
                }

                Files.createDirectory(folder);
                return "Folder created: " + arg;

            case "create":
                if (!validName(arg)) {
                    return "Usage: create <filename.txt>";
                }

                Path file = resolvePath(arg);

                if (Files.exists(file)) {
                    return "Error: File already exists.";
                }

                Files.createFile(file);
                return "File created: " + arg;

            case "cat":
                if (!validName(arg)) {
                    return "Usage: cat <filename>";
                }

                Path readFile = resolvePath(arg);

                if (!Files.isRegularFile(readFile)) {
                    return "Error: File not found.";
                }

                return Files.readString(readFile, StandardCharsets.UTF_8);

            case "date":
                return LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(
                                "dd-MM-yyyy HH:mm:ss"));

            case "whoami":
                return System.getProperty("user.name");

            case "clear":
                return "_CLEAR_";

            case "exit":
                return "JavaShell session is still running.";

            default:
                return "Unknown command: " + cmd +
                        "\nType 'help' to see available commands.";
        }
    }

    private static boolean validName(String name) {
        if (name.isBlank()) {
            return false;
        }

        Path path = Paths.get(name);

        return !path.isAbsolute()
                && path.getNameCount() == 1
                && !name.equals(".")
                && !name.equals("..")
                && !name.contains("/")
                && !name.contains("\\");
    }

    private static Path resolvePath(String name) {
        return currentDir.resolve(name).normalize();
    }
}