#!/usr/bin/env bash
# Stop hook: when the uncommitted diff has changed since the last review, keep Claude going so it
# runs the reviewer subagent on it and then brings the derived docs (README, PLAN) in line.
set -euo pipefail

input=$(cat)

# Without jq the hook can't read its input or answer; skip rather than fail every stop.
command -v jq >/dev/null || exit 0

# Claude is already continuing because of this hook; let it stop (prevents a loop).
if [ "$(jq -r '.stop_hook_active // false' <<<"$input")" = "true" ]; then
  exit 0
fi

cd "${CLAUDE_PROJECT_DIR:-.}"
git rev-parse --git-dir >/dev/null 2>&1 || exit 0

# Only code and config count: Markdown docs are left out, otherwise syncing the docs after a review
# would change the diff and ask for another review on the next stop.
paths=(. ':(exclude)*.md')

# Nothing uncommitted: nothing to review.
if [ -z "$(git status --porcelain -- "${paths[@]}")" ]; then
  exit 0
fi

# Fingerprint of everything uncommitted: tracked changes plus the contents of untracked files
# (gitignored files are left out; unreadable ones, such as broken symlinks, count by name only).
diff_hash=$(
  {
    git diff HEAD --binary -- "${paths[@]}"
    git ls-files --others --exclude-standard -z -- "${paths[@]}" | while IFS= read -r -d '' f; do
      printf '== %s\n' "$f"
      cat -- "$f" 2>/dev/null || true
    done
  } | shasum -a 256 | cut -d' ' -f1
)

state_file="$(git rev-parse --git-dir)/claude-last-reviewed-diff"
if [ -f "$state_file" ] && [ "$(cat "$state_file")" = "$diff_hash" ]; then
  exit 0
fi

# The diff counts as reviewed once the review is asked for, so an interrupted turn isn't asked again.
jq -n '{
  decision: "block",
  reason: "The uncommitted diff changed since the last review. Run the `reviewer` subagent on it (git diff HEAD plus untracked files) and report its findings to the user; do not apply fixes without asking. Then update README.md and docs/PLAN.md yourself wherever they no longer match the code. If the code disagrees with INTENT.md or CLAUDE.md (any section), report it and ask; edit neither without approval."
}' && echo "$diff_hash" >"$state_file"
