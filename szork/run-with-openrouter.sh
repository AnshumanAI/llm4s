#!/bin/bash

# Szork OpenRouter Configuration Script
# This script sets up the environment variables needed to run Szork with OpenRouter API

echo "🎮 Szork OpenRouter Configuration"
echo "=================================="

# Check if API key is provided
if [ -z "$1" ]; then
    echo "❌ Error: Please provide your OpenRouter API key as an argument"
    echo "Usage: ./run-with-openrouter.sh YOUR_API_KEY [MODEL_NAME]"
    echo ""
    echo "Example:"
    echo "  ./run-with-openrouter.sh sk-or-v1-abc123..."
    echo "  ./run-with-openrouter.sh sk-or-v1-abc123... openai/gpt-4o"
    echo ""
    echo "Available models:"
    echo "  openai/gpt-4o (default)"
    echo "  openai/gpt-4o-mini"
    echo "  anthropic/claude-3-5-sonnet-20241022"
    echo "  google/gemini-pro"
    exit 1
fi

# Set API key
export OPENAI_API_KEY="$1"

# Set base URL to OpenRouter
export OPENAI_BASE_URL="https://openrouter.ai/api/v1"

# Set model (default to gpt-4o if not provided)
MODEL=${2:-"openai/gpt-4o"}
export LLM_MODEL="$MODEL"

echo "✅ Configuration set:"
echo "   API Key: ${OPENAI_API_KEY:0:10}..."
echo "   Base URL: $OPENAI_BASE_URL"
echo "   Model: $LLM_MODEL"
echo ""

# Test the configuration
echo "🧪 Testing OpenRouter connection..."
TEST_RESPONSE=$(curl -s -X POST https://openrouter.ai/api/v1/chat/completions \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "'$MODEL'",
    "messages": [{"role": "user", "content": "Hello! Just testing the connection."}],
    "max_tokens": 10
  }' 2>/dev/null)

if echo "$TEST_RESPONSE" | grep -q "choices"; then
    echo "✅ OpenRouter connection successful!"
else
    echo "❌ OpenRouter connection failed. Please check your API key."
    echo "Response: $TEST_RESPONSE"
    exit 1
fi

echo ""
echo "🚀 Starting Szork with OpenRouter..."
echo "   Frontend will be available at: http://localhost:3090"
echo "   Backend will be available at: http://localhost:8090"
echo ""
echo "Press Ctrl+C to stop the game"
echo ""

# Start the game
cd "$(dirname "$0")"
sbt run