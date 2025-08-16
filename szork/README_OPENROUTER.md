# Szork with OpenRouter API

This guide shows you how to configure Szork to work with OpenRouter API instead of OpenAI directly.

## Why OpenRouter?

OpenRouter provides access to multiple LLM models through a single API, including:
- OpenAI models (GPT-4, GPT-3.5)
- Anthropic models (Claude)
- Google models (Gemini)
- And many more!

## Setup Instructions

### 1. Get OpenRouter API Key

1. Visit [OpenRouter](https://openrouter.ai/)
2. Sign up and get your API key
3. Note your API key for the next step

### 2. Set Environment Variables

Set these environment variables before running the game:

```bash
# Required: Your OpenRouter API key
export OPENAI_API_KEY=your_openrouter_api_key_here

# Required: Set the base URL to OpenRouter
export OPENAI_BASE_URL=https://openrouter.ai/api/v1

# Required: Choose your model (examples below)
export LLM_MODEL=openai/gpt-4o
# OR
export LLM_MODEL=anthropic/claude-3-5-sonnet-20241022
# OR
export LLM_MODEL=google/gemini-pro
# OR any other model available on OpenRouter
```

### 3. Run the Game

```bash
# Start the backend
cd szork
sbt run

# In another terminal, start the frontend
cd szork/frontend
npm run dev
```

## Available Models

You can use any model available on OpenRouter. Here are some popular options:

### OpenAI Models
- `openai/gpt-4o` - Latest GPT-4 model
- `openai/gpt-4o-mini` - Faster, cheaper GPT-4
- `openai/gpt-3.5-turbo` - Good balance of speed and quality

### Anthropic Models
- `anthropic/claude-3-5-sonnet-20241022` - Latest Claude model
- `anthropic/claude-3-opus-20240229` - Most capable Claude model
- `anthropic/claude-3-haiku-20240307` - Fastest Claude model

### Google Models
- `google/gemini-pro` - Google's Gemini Pro model
- `google/gemini-flash-1.5` - Fast Gemini model

### Other Models
- `meta-llama/llama-3.1-8b-instruct` - Meta's Llama model
- `mistralai/mistral-7b-instruct` - Mistral's 7B model

## Example Configuration Script

Create a file called `run-with-openrouter.sh`:

```bash
#!/bin/bash

# OpenRouter Configuration
export OPENAI_API_KEY=your_openrouter_api_key_here
export OPENAI_BASE_URL=https://openrouter.ai/api/v1
export LLM_MODEL=openai/gpt-4o

# Start the game
cd szork
sbt run
```

Make it executable and run:
```bash
chmod +x run-with-openrouter.sh
./run-with-openrouter.sh
```

## Troubleshooting

### Common Issues

1. **"OPENAI_API_KEY not set"**
   - Make sure you've set the `OPENAI_API_KEY` environment variable
   - Verify your OpenRouter API key is correct

2. **"Model not supported"**
   - Check that your `LLM_MODEL` format is correct (e.g., `openai/gpt-4o`)
   - Verify the model is available on OpenRouter

3. **API errors**
   - Check your OpenRouter account for usage limits
   - Verify your API key has sufficient credits

### Testing Your Configuration

You can test if your OpenRouter setup is working by running:

```bash
export OPENAI_API_KEY=your_key
export OPENAI_BASE_URL=https://openrouter.ai/api/v1
export LLM_MODEL=openai/gpt-4o

# Test with a simple curl request
curl -X POST https://openrouter.ai/api/v1/chat/completions \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "openai/gpt-4o",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

## Cost Optimization

OpenRouter allows you to choose from different models with varying costs:

- **Budget-friendly**: `openai/gpt-3.5-turbo`, `anthropic/claude-3-haiku-20240307`
- **Balanced**: `openai/gpt-4o-mini`, `anthropic/claude-3-5-sonnet-20241022`
- **High-quality**: `openai/gpt-4o`, `anthropic/claude-3-opus-20240229`

Check [OpenRouter's pricing page](https://openrouter.ai/pricing) for current rates.

## Security Notes

- Never commit your API key to version control
- Use environment variables or a `.env` file (not included in git)
- Consider using OpenRouter's project-based API keys for better security