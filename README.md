# LifeOS 🧠
**Team:** MSRIT_CrimsonLobster  
**Theme:** Daily Utility / Productivity

## Problem
People juggle dozens of apps for tasks, reminders, and notes. Every simple action requires multiple taps and app switches — creating cognitive overload and killing productivity.

## Solution
LifeOS is a unified AI-powered Android assistant. One app, one input line — type or speak naturally, and LifeOS handles the rest. Tasks, reminders, and memory — all local, all fast.

## Features
- 🤖 **AI Chat** — Natural language intent parsing via Llama 3.3 (Groq)
- 📌 **Task Manager** — Add, edit, delete, categorize tasks (Pending / Done)
- 🎙️ **Voice Input** — Speak your tasks or questions hands-free
- 🔔 **Smart Reminders** — Set alarms via chat or time picker
- 🔊 **Text-to-Speech** — App reads responses and confirmations aloud
- 📊 **Dashboard** — Live count of pending, completed, and total tasks
- 🔒 **Privacy-First** — All data stored locally on-device via SQLite

## Tech Stack
| Layer | Technology |
|-------|-----------|
| Mobile App | Android (Kotlin) |
| Local DB | SQLite (via DatabaseHelper) |
| AI Backend | Node.js + Groq SDK |
| AI Model | Llama 3.3 70B (via Groq API) |
| Voice | Android SpeechRecognizer + TextToSpeech |
| Notifications | Android AlarmManager + BroadcastReceiver |
| Tunnel | ngrok |

## Setup & Installation

### Prerequisites
- Android Studio
- Node.js v18+
- Groq API Key
- ngrok account

### Backend
```bash
cd server
npm install
node server.js
```
Then expose it:
```bash
ngrok http 3005
```
Update this line in `MainActivity.kt` with your ngrok URL:
```kotlin
val url = "https://YOUR-NGROK-URL.ngrok-free.app/chat"
```
> ⚠️ The ngrok URL changes every time you restart ngrok. Always update this line before running the app.

### Android App
1. Open project in Android Studio
2. Update the ngrok URL in `MainActivity.kt`
3. Build & run on device or emulator (API 26+)

## Usage
| Action | How |
|--------|-----|
| Add a task | Type/speak in Diary screen → tap Pending or Done |
| Ask AI | Type in main chat → tap Send |
| Set reminder via AI | Type "Remind me at 6pm" → AI sets alarm |
| Set reminder manually | Tap 🔔 → pick time |
| Edit task | Tap the task card |
| Delete task | Long press the task card |

## Project Structure
```
app/
├── MainActivity.kt        # Home screen, AI chat, dashboard
├── DiaryActivity.kt       # Task manager, voice input
├── DatabaseHelper.kt      # SQLite CRUD operations
├── ReminderReceiver.kt    # Notification broadcast receiver
server/
├── server.js              # Node.js AI backend (Groq/Llama)
├── package.json
```
