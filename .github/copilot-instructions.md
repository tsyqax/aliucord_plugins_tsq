# Git Commit Message Generation Instructions

You must generate git commit messages strictly following the Conventional Commits specification.

## Core Rules
1. **Format**: Use `<type>(<scope>): <description>` or `<type>: <description>` if the scope is broad or unclear.
2. **Case**: The `<type>` and `<description>` must start with a lowercase letter.
3. **Mood**: Use the imperative, present tense for the description (e.g., "add", "fix", "change", "remove", instead of "added", "fixes", "changing").
4. **Punctuation**: Do not end the commit message with a period.
5. **Language**: Always write the commit message in English.

## Allowed Types
- **feat**: A new feature
- **fix**: A bug fix
- **docs**: Documentation only changes
- **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
- **refactor**: A code change that neither fixes a bug nor adds a feature
- **perf**: A code change that improves performance
- **test**: Adding missing tests or correcting existing tests
- **chore**: Changes to the build process or auxiliary tools and libraries such as documentation generation

## Examples
- `feat(deco): implement profile decoration`
- `fix(auth): resolve crash on login screen`
- `chore(config): update tailwind settings`
- `refactor(user): clean up redundant loops`
