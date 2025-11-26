#!/usr/bin/env bash

# Simple helper to call the chatbot REST API with a single prompt argument.

set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 \"your question here\"" >&2
  exit 1
fi

QUESTION="$*"
HOST="${CHAT_HOST:-http://localhost:8080}"
ENDPOINT="${HOST%/}/chat"

curl -sS -X POST \
  -H "Content-Type: text/plain" \
  --data "$QUESTION" \
  "$ENDPOINT"
