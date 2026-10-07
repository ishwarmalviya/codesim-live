#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! command -v ngrok >/dev/null 2>&1; then
  echo "ngrok is not installed. Install with: brew install ngrok"
  exit 1
fi

if ! lsof -i :5173 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Frontend is not running on port 5173."
  echo "Start it first: cd codesim-frontend && VITE_NGROK=1 npm run dev"
  exit 1
fi

if ! lsof -i :8080 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Backend is not running on port 8080."
  echo "Start it first: cd codesim-backend && JAVA_HOME=\$(/usr/libexec/java_home -v 17) mvn spring-boot:run"
  exit 1
fi

if pgrep -f "ngrok.*5173" >/dev/null 2>&1; then
  echo "ngrok is already running for port 5173."
else
  NGROK_USER_CONFIG="$HOME/Library/Application Support/ngrok/ngrok.yml"
  if [ -f "$NGROK_USER_CONFIG" ]; then
    ngrok http 5173 --config="$NGROK_USER_CONFIG" --config="$ROOT/ngrok.yml" --log=stdout &
  else
    echo "No ngrok authtoken found. Run: ngrok config add-authtoken <your-token>"
    echo "Get one at: https://dashboard.ngrok.com/get-started/your-authtoken"
    exit 1
  fi
  sleep 2
fi

PUBLIC_URL="$(curl -s http://127.0.0.1:4040/api/tunnels | python3 -c "
import json, sys
data = json.load(sys.stdin)
for t in data.get('tunnels', []):
    if t.get('proto') == 'https':
        print(t['public_url'])
        break
" 2>/dev/null || true)"

echo ""
echo "CodeSim is shareable via ngrok."
if [ -n "$PUBLIC_URL" ]; then
  echo "Public URL: $PUBLIC_URL"
else
  echo "Open http://127.0.0.1:4040 for the ngrok dashboard and public URL."
fi
echo "Local dashboard: http://127.0.0.1:4040"
echo ""
echo "Share the public URL with others. API traffic is proxied through Vite to localhost:8080."
