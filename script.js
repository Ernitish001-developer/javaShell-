"use strict";

const BACKEND_URL = "http://127.0.0.1:8080/api/command";

const terminal = document.getElementById("terminal");
const commandInput = document.getElementById("command");
const commandForm = document.getElementById("commandForm");
const runButton = document.getElementById("runButton");
const clearButton = document.getElementById("clearButton");
const commandCount = document.getElementById("commandCount");
const sessionStatus = document.getElementById("sessionStatus");
const currentPath = document.getElementById("currentPath");
const historyList = document.getElementById("historyList");
const fileList = document.getElementById("fileList");

let totalCommands = 0;
let commandHistory = [];
let historyIndex = 0;

// Display output in terminal
function printOutput(message, className) {
    const line = document.createElement("div");

    line.className = "output-line " + (className || "output-text");
    line.textContent = String(message);

    terminal.appendChild(line);
    terminal.scrollTop = terminal.scrollHeight;
}

// Clear terminal
function clearTerminal() {
    terminal.replaceChildren();

    printOutput("Terminal cleared.", "success-text");
}

// Update connection status
function setStatus(message) {
    sessionStatus.textContent = message;
}

// Update command history
function addHistory(command) {
    commandHistory.push(command);
    historyIndex = commandHistory.length;

    const item = document.createElement("li");
    item.textContent = command;

    historyList.appendChild(item);
}

// Send command to Java backend
async function sendCommand(command) {
    const response = await fetch(BACKEND_URL, {
        method: "POST",
        headers: {
            "Content-Type": "text/plain"
        },
        body: command
    });

    const result = await response.text();

    if (!response.ok) {
        throw new Error(result || "HTTP error " + response.status);
    }

    return result;
}

// Execute a command
async function runCommand(commandText) {
    const command = String(commandText || "").trim();

    if (command === "") {
        return;
    }

    printOutput("$ " + command, "command-line");
    addHistory(command);

    totalCommands++;
    commandCount.textContent = String(totalCommands);

    commandInput.value = "";
    runButton.disabled = true;

    setStatus("Working...");

    try {
        // Clear terminal locally
        if (command.toLowerCase() === "clear") {
            clearTerminal();
            setStatus("Connected");
            return;
        }

        // Send all other commands to Java
        const result = await sendCommand(command);

        if (result === "_CLEAR_") {
            clearTerminal();
        } else {
            printOutput(result || "(No output)");
        }

        // Update current directory after pwd
        if (command.toLowerCase() === "pwd") {
            currentPath.textContent = result;
        }

        // Update directory information after cd
        if (command.toLowerCase().startsWith("cd ")) {
            try {
                const path = await sendCommand("pwd");
                currentPath.textContent = path;
            } catch (error) {
                console.error(error);
            }
        }

        // Refresh files after file operations
        const firstWord = command.split(/\s+/)[0].toLowerCase();

        if (
            firstWord === "cd" ||
            firstWord === "mkdir" ||
            firstWord === "create"
        ) {
            await refreshFiles(false);
        }

        setStatus("Connected");

    } catch (error) {
        printOutput(
            "Backend connection failed. Please start ShellServer.java.",
            "error-text"
        );

        setStatus("Disconnected");
        console.error("JavaShell error:", error);

    } finally {
        runButton.disabled = false;
        commandInput.focus();
    }
}

// Load files from current directory
async function refreshFiles(showMessage) {
    fileList.replaceChildren();

    try {
        const directory = await sendCommand("pwd");
        const listing = await sendCommand("dir");

        currentPath.textContent = directory;

        if (showMessage) {
            printOutput("File Manager refreshed.", "success-text");
        }

        if (listing === "Directory is empty.") {
            const emptyMessage = document.createElement("p");

            emptyMessage.textContent = "This directory is empty.";
            fileList.appendChild(emptyMessage);

            return;
        }

        const lines = listing.split("\n");

        lines.forEach(function (line) {
            if (line.trim() === "") {
                return;
            }

            const item = document.createElement("div");
            item.className = "file-item";

            const icon = document.createElement("span");
            icon.className = "file-icon";

            if (line.startsWith("[DIR]")) {
                icon.textContent = "📁";
            } else {
                icon.textContent = "📄";
            }

            const name = document.createElement("span");

            name.textContent = line.replace(/^\[(DIR|FILE)\]\s*/, "");

            item.appendChild(icon);
            item.appendChild(name);

            fileList.appendChild(item);
        });

    } catch (error) {
        const errorMessage = document.createElement("p");

        errorMessage.textContent =
            "Unable to load files. Check the Java backend.";

        fileList.appendChild(errorMessage);

        setStatus("Disconnected");

        console.error("File Manager error:", error);
    }
}

// Run command when form is submitted
commandForm.addEventListener("submit", function (event) {
    event.preventDefault();
    runCommand(commandInput.value);
});

// Clear button
clearButton.addEventListener("click", function () {
    clearTerminal();
    commandInput.focus();
});

// Quick command buttons
document.querySelectorAll("[data-command]").forEach(function (button) {
    button.addEventListener("click", function () {
        runCommand(button.dataset.command);
    });
});

// Refresh File Manager
document.getElementById("refreshFiles").addEventListener(
    "click",
    function () {
        refreshFiles(true);
    }
);

// Clear command history
document.getElementById("clearHistory").addEventListener(
    "click",
    function () {
        commandHistory = [];
        historyIndex = 0;

        historyList.replaceChildren();
    }
);

// Arrow key command history
commandInput.addEventListener("keydown", function (event) {
    if (event.key === "ArrowUp") {
        event.preventDefault();

        if (historyIndex > 0) {
            historyIndex--;

            commandInput.value = commandHistory[historyIndex];
        }
    }

    if (event.key === "ArrowDown") {
        event.preventDefault();

        if (historyIndex < commandHistory.length - 1) {
            historyIndex++;

            commandInput.value = commandHistory[historyIndex];
        } else {
            historyIndex = commandHistory.length;
            commandInput.value = "";
        }
    }
});

// Sidebar navigation
document.querySelectorAll(".nav-btn").forEach(function (button) {
    button.addEventListener("click", function () {
        const view = button.dataset.view;

        document.getElementById("terminalView")
            .classList.toggle("hidden", view !== "terminal");

        document.getElementById("filesView")
            .classList.toggle("hidden", view !== "files");

        document.getElementById("pageTitle").textContent =
            view === "files" ? "File Manager" : "Terminal";

        document.querySelectorAll(".nav-btn").forEach(
            function (navButton) {
                navButton.classList.toggle(
                    "active",
                    navButton === button
                );
            }
        );

        if (view === "files") {
            refreshFiles(false);
        } else {
            commandInput.focus();
        }
    });
});

// Initial terminal messages
printOutput("Welcome to JavaShell!", "success-text");
printOutput("Java backend command interface.");
printOutput("Type 'help' to see available commands.");

// Initial connection and File Manager check
refreshFiles(false);

commandInput.focus();
// JavaShell Welcome Screen

document.addEventListener("DOMContentLoaded", function () {

    const welcomeScreen =
        document.getElementById("welcome-screen");

    const startButton =
        document.getElementById("get-started");

    if (welcomeScreen && startButton) {

        startButton.addEventListener("click", function () {

            welcomeScreen.classList.add("hide");

            setTimeout(function () {
                welcomeScreen.style.display = "none";
            }, 500);

        });

    }

});