# ASHIRU AI — Offline Brain Architecture

## Goal
Ship a useful Hausa/English assistant whose curated knowledge and personal memory remain available offline on Android.

## Four separate layers
1. **Knowledge packs (GitHub source):** reviewed Hausa/English Q&A and lessons, versioned with the source code.
2. **On-device knowledge index:** packaged JSON/assets searched locally. New app versions may add or correct lessons.
3. **Personal memory (private phone storage):** SQLite database in Android app-private storage. User facts, preferences and user-approved corrections are separate from the public knowledge pack.
4. **Reasoning / response layer:** retrieves relevant local knowledge, considers conversation context, and answers with uncertainty when evidence is missing.

## Memory rules
- Never upload personal memories to GitHub.
- Never overwrite personal memory when updating knowledge packs.
- Provide view, edit, export and delete controls.
- Store only information the user asks the assistant to remember or clearly approves.
- Keep the database in app-private storage; use Android backup policy deliberately.
- Knowledge retrieval must work with airplane mode enabled.

## What offline means
Offline mode can answer from bundled lessons, local memory and any on-device model installed. It cannot fetch live news, current prices, remote maps or cloud AI responses without a network. Speech recognition and text-to-speech availability depend on Android language packs installed on the device.

## Model strategy
Start with retrieval over a curated offline knowledge pack and deterministic local skills. Later evaluate a small on-device language model only after measuring APK/model size, RAM, storage, speed, Hausa quality and supported CPU/NNAPI. Do not claim full neural-model training merely because the app stores new memories.

## Planned skill modules
- Hausa/English tutor and conversation
- General knowledge and study coach
- Math and practical calculations
- Android phone helper
- Programming and debugging tutor
- Business planning and bookkeeping basics
- Digital security and scam awareness
- Notes, reminders and task planning
- File and media organization
- Permission-based app actions

## Build and validation
Each pack should be validated as JSON in CI, packaged into app assets, and tested with representative Hausa queries. Include an offline test that disables network access and confirms knowledge search still returns results.
