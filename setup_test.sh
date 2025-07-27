#!/bin/bash

echo "🎤 TTS/ASR Testing Setup"
echo "========================="

echo ""
echo "📋 Available Free Options:"
echo "1. ElevenLabs (Recommended) - 10,000 chars/month free"
echo "2. OpenAI - $5 credit free (requires card)"
echo "3. Google Cloud - 60 minutes/month free"

echo ""
echo "🚀 Quick Setup for ElevenLabs:"
echo "1. Go to https://elevenlabs.io"
echo "2. Sign up for free account"
echo "3. Go to Profile → API Key"
echo "4. Copy your API key"

echo ""
echo "🔑 Set your API key:"
echo "export ELEVENLABS_API_KEY='your-api-key-here'"
echo "export SPEECH_MODEL='elevenlabs/eleven_monolingual_v1'"

echo ""
echo "🧪 Test TTS:"
echo "sbt 'runMain TestTTS'"

echo ""
echo "🎵 Test ASR (requires audio file):"
echo "1. Record a short audio clip"
echo "2. Save as 'sample_audio.mp3'"
echo "3. Set OPENAI_API_KEY"
echo "4. Run: sbt 'runMain TestASR'"

echo ""
echo "📁 Test files created:"
echo "- test_tts.scala (TTS test)"
echo "- test_asr.scala (ASR test)"
echo "- setup_test.sh (this script)"