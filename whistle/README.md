# Cactus Whistle native runtime

This module links the prebuilt Cactus 4.0.0 Android ARM64 runtime to a small JNI bridge.
It contains no Cactus source code.

Bundled artifacts:

| File | Source | SHA-256 |
| --- | --- | --- |
| `src/main/jni/arm64-v8a/libcactus.a` | `https://huggingface.co/Cactus-Compute/cactus/resolve/main/releases/4.0.0/android-arm64/libcactus.a` | `4f75e975f6a859979a2715a89fdbc0fc32742cde001f8674598cf89017d39fdf` |
| `src/main/jni/arm64-v8a/cactus.h` | `https://huggingface.co/Cactus-Compute/cactus/resolve/main/releases/4.0.0/android-arm64/cactus.h` | upstream |
| `../app/src/main/assets/models/whistle/whistle.cact` | `https://huggingface.co/Cactus-Compute/whistle/resolve/main/whistle.cact` | `b6e02f048568ac5d01a2042556c658061e699acbc0aa2a1439f52f3d461dffeb` |

Cactus and Whistle are Apache-2.0 licensed. See `../LICENSE.cactus.txt`.

The runtime is published for `android-arm64` (and `android-armv7`/`android-riscv64`). The app keeps
`x86_64` for the emulator, where `CactusWhistleNative.isAvailable()` returns false and transcription
falls back to whisper.cpp.

Update the artifacts by downloading the same paths from the pinned release:

```sh
curl -L -o whistle/src/main/jni/arm64-v8a/libcactus.a \
  https://huggingface.co/Cactus-Compute/cactus/resolve/main/releases/4.0.0/android-arm64/libcactus.a
curl -L -o whistle/src/main/jni/arm64-v8a/cactus.h \
  https://huggingface.co/Cactus-Compute/cactus/resolve/main/releases/4.0.0/android-arm64/cactus.h
curl -L -o app/src/main/assets/models/whistle/whistle.cact \
  https://huggingface.co/Cactus-Compute/whistle/resolve/main/whistle.cact
```
