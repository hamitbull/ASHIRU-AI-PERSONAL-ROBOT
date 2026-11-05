# ASHIRU AI Knowledge Packs

This folder is the source-of-truth for curated knowledge shipped with the robot.

## How the offline learning design works
- Curated lessons and verified facts are authored here in version control.
- A build step will package selected language packs into Android assets.
- The app will search the local pack without internet.
- User-specific memories are stored separately in the app's private SQLite database on the phone.
- Updating the knowledge pack must never overwrite or delete personal memories.

## Knowledge areas
1. Hausa and English communication
2. Learning, reasoning and problem solving
3. Android and phone literacy
4. Computer science and programming
5. Digital safety and privacy
6. Business, money basics and productivity
7. Mathematics and everyday science
8. General knowledge and practical life skills

## Important distinction
Adding lessons here does not train neural-network weights. It creates a searchable offline knowledge library. A local language model may be integrated later if the device has enough storage, RAM and processing power. The assistant must clearly say when it does not know something and avoid inventing facts.

## Contribution format
Each knowledge item should have a stable id, category, language, question examples, answer, and tags. Keep answers concise, useful, safe, and reviewable. Never put API keys, passwords, private user data, or personal memories in this repository.
