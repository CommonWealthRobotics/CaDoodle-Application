# Git Policy for Bots/AI Agents

## Rule: Read-Only Access

Bots and AI agents may **read** git history, logs, status, and diffs.

Bots and AI agents must **never** perform any git write operations, including but not limited to:

- `git add`
- `git commit`
- `git push`
- `git pull`
- `git merge`
- `git rebase`
- `git reset`
- `git revert`
- `git tag`
- `git clean`
- `git rm`
- `git mv`
- `git gc`
- Any command that modifies repo state or history

If a write operation is needed, inform the user and ask them to perform it manually.
