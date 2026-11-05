# TruthLens — Sprint 1

This is the Sprint 1 baseline of our news-verification group project.

Implemented stories:
- US-01: User registration with validation, duplicate-email checks and password hashing.
- US-02: User login/logout with server-side sessions and access checks.
- US-03: Private verification history with date/status filters and owner-scoped report access.
- US-05: Article text submission with validation and the configured input limit.
- US-06: Public article URL submission with extraction and destination checks.

This version stores submissions and prepares text metadata. It does not perform AI fact checking. Sprint 2 claim/evidence/verdict changes and the later Python AI agent are not included. Existing admin and news interface features from the baseline are retained.

React and TypeScript provide the frontend, Java/Spring Boot provides the backend, and SQL migrations define the database. The local preview uses persistent H2; PostgreSQL configuration is provided for deployment.

Requirements: Node.js 22+, Java 17+ and Maven. Run these commands from this folder:

```powershell
npm install
npm run dev
```

The source contains no local database, private credentials, installed packages or model files. Existing private account data is not part of this export.

## Step-by-step instructions

Follow these steps to run the website on your laptop.

## 1. Open the project folder

1. Open VS Code.
2. Click **File → Open Folder**.
3. Select the TruthLens folder.

Check the left side of VS Code. You should see these three folders together:

- frontend
- backend
- database

You should also see a file named **package.json**. Do not open only the frontend folder.

## 2. Open a terminal

At the top of VS Code, click **Terminal → New Terminal**.

A panel opens at the bottom. This is where you type the commands below.

If an older copy of TruthLens is already running in another terminal, stop it with **Ctrl+C** first.

## 3. Install the project packages

Type this in the terminal and press Enter:

```powershell
npm install
```

Wait until it finishes and you can type another command.

You normally do this only once after downloading the project. You need an internet connection for the first installation.

## 4. Start TruthLens

In the same terminal, type this and press Enter:

```powershell
npm run dev
```

This starts both parts of the application:

- The website that you see and click.
- The Java program that handles accounts and saves submissions.

Wait until the terminal says **Started TruthLensApplication**. The first start may take longer because it downloads Java libraries.

## 5. Open the website

Open Chrome or another browser. Paste this address into the address bar:

http://127.0.0.1:5173

For the administrator login, use:

http://127.0.0.1:5173/admin

Keep the VS Code terminal open while using the website.

## How to stop it

Click inside the terminal and press **Ctrl+C**.

## How to start it next time

Open the same project folder in VS Code, open a terminal, and run:

```powershell
npm run dev
```

You do not normally need to run npm install again.

## If it does not start

- **Cannot find package.json:** you opened the wrong folder. Open the folder containing frontend, backend, database and package.json.
- **Port already in use:** another copy is running. Stop that copy before starting this one.
- **npm is not recognized:** Node.js must be installed.
- **Java or Maven is missing:** install Java 17 or newer and Maven, then restart VS Code. Node.js 22 or newer is required for this project.

If you are unsure, copy the terminal error instead of changing the code.

## What the folders mean

- **frontend:** the website's appearance and buttons.
- **backend:** login, accounts, submissions and other server work.
- **database:** instructions that create the database tables.

The normal local run creates and uses its database automatically. You do not need to set up PostgreSQL to try the website on your laptop.

This version saves and prepares articles. It does not perform AI fact-checking yet.
