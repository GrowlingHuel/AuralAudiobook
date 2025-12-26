# Acoustic Alchemy: Next-Gen TTS

**Acoustic Alchemy** is a high-fidelity local text-to-speech engine for Android, powered by the **Kokoro-82M** model. It features a unique "Nebula Voice Lab" that allows users to seamlessly blend different voice personalities in real-time.

## Features

### 🌌 Nebula Voice Lab
- **Interactive Blending**: Drag the central "Probe" across the Nebula to morph the voice style between three distinct personalities:
    - **Adam**: Deep, authoritative, neutral.
    - **Bella**: Soft, higher pitch, soothing.
    - **Sky**: Dynamic, expressive, energetic.
- **Visual Feedback**: The interface pulses and glows in sync with audio playback.

### 📚 Literature Reader
- **Moby Dick Integration**: Includes a built-in reader for Herman Melville's classic *Moby Dick*.
- **Instant Synthesis**: Generates audio locally on-device using ONNX Runtime.

### 🔧 Technology
- **Model**: Kokoro-82M (v1.0)
- **Engine**: ONNX Runtime with quantization.
- **Audio**: 24kHz High-Fidelity Mono Output.

## Technical Highlights
- **Phoneme Alignment**: Custom IPA-based tokenization pipeline ensures perfect pronunciation.
- **100% Local**: No internet connection required for synthesis.
- **Android 15 Ready**: Optimized for the latest Android memory and alignment requirements.

## Usage
1. Open the App to enter the **Nebula Voice Lab**.
2. Drag the **White Probe** to your desired mix of voices.
3. Click **"Listen to First Page"** to hear the opening lines of *Moby Dick* synthesized in your custom voice.

---
*Built with love by Antigravity*
