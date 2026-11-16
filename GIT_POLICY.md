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

## Rule: Detached HEAD

Before writing or creating files inside any git repository, the agent must check
that the repository is on a named branch (`git branch --show-current` returns a
non-empty value).

If the repository is in a **detached HEAD** state, the agent must:

1. **Stop** and pause the work at that point.
2. **Warn the user** that files written in a detached HEAD state are not held by
   any branch and will be orphaned or lost on the next checkout.
3. **Wait** for the user to check out a branch (for example
   `git checkout development`) before continuing.

Writing files into a detached HEAD repository without this warning and pause is
a policy violation, even when the write itself is not a git command.
