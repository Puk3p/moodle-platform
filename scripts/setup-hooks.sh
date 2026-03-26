#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "Setting up Git hooks..."

git config core.hooksPath .githooks

chmod +x "$PROJECT_ROOT/.githooks/pre-commit"
chmod +x "$PROJECT_ROOT/.githooks/pre-push"
chmod +x "$PROJECT_ROOT/.githooks/commit-msg"

echo "✓ Git hooks installed. Using .githooks/ directory."
echo ""
echo "Hooks enabled:"
echo "  • pre-commit  — branch naming, forbidden patterns, Spotless, TS check"
echo "  • commit-msg  — Conventional Commits format enforcement"
echo "  • pre-push    — Spotless, SpotBugs, backend tests, frontend build & tests"
