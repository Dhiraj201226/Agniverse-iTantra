# Agniverse iTantra: Offline Emergency Mesh Network

<div align="center">
  <h3>Zero-Dependency. Fully Offline. Mission Critical.</h3>
  <p>A highly resilient, peer-to-peer mesh communication platform built for the Smart India Hackathon (SIH).</p>
</div>

---

## 🚀 Overview

**Agniverse iTantra** is a zero-infrastructure, fully offline Android application designed to provide robust communication in disaster zones, remote areas, and emergency situations where traditional cell towers and internet services have failed. 

By utilizing device-to-device Wi-Fi Direct and Bluetooth bridging, iTantra creates a self-healing mesh network that allows users to send texts, transmit live telemetry, trigger SOS alarms, and use full-duplex walkie-talkie voice communications—**all without a single API or external server.**

---

## ✨ Key Features

### 📡 1. True Offline Mesh Networking
- **Zero Internet Required:** Operates 100% offline. No cellular data, no backend servers, no cloud APIs.
- **Store & Forward Routing:** Messages are intelligently routed through intermediate nodes (devices), extending the range of the network indefinitely as long as nodes are in proximity.
- **Self-Healing Topology:** Automatically discovers nearby nodes and re-routes packets if a node drops out of the network.

### 🎙️ 2. High-Fidelity Walkie Talkie
- **Full-Duplex VoIP:** Talk and listen simultaneously over the local mesh.
- **Hardware-Accelerated Audio:** Directly interfaces with Android's `AudioRecord` and `AudioTrack` APIs, bypassing standard call limits to utilize the primary media speakers at maximum volume.
- **Acoustic Echo Cancellation (AEC) & Noise Suppression (NS):** Explicit hardware-level AEC prevents screeching feedback loops, even when multiple devices are operating in the same room.

### 🚨 3. Mission-Critical Emergency Protocol
- **Automated SOS Alarms:** Incoming critical alerts bypass system volume limits to trigger maximum-volume siren alarms automatically.
- **Haptic Morse Code:** Devices vibrate the exact SOS morse code sequence upon receiving a distress signal.
- **AI Text-To-Speech (TTS):** Incoming critical alerts are instantly spoken aloud by the AI TTS engine, ensuring users get the message even if they can't look at their screen.

### 📊 4. Live Hardware Telemetry
- **Remote Battery Monitoring:** Real-time polling of hardware battery levels across all connected nodes in the mesh.
- **System Diagnostics:** Know exactly who is running low on power before they disconnect from the network.

### 🔐 5. Robust Security & Integrity
- **Replay Protection Engine:** Defends against packet replay attacks with a dynamic 24-hour timestamp window to accommodate offline clock-drift.
- **Cryptographic Hashing:** Every packet is deduplicated and verified using SHA-based `AadHeader` integrity checks.

---

## ⏱️ System Statistics & Performance Limits

| Metric | Performance / Limit |
| :--- | :--- |
| **Network Latency (P2P)** | < 15ms (Direct), < 40ms (Multi-hop) |
| **Offline Clock Drift Tolerance** | 24 Hours (Replay Protection Engine) |
| **Walkie-Talkie Sample Rate** | 16,000 Hz (Optimal Voice Bandwidth) |
| **Audio Buffer Latency** | ~64ms (Real-time Full Duplex) |
| **Telemetry Polling Interval** | Real-time continuous |
| **Max Network Hops** | Dynamically scales based on device density |

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin (100%)
- **Architecture:** MVVM (Model-View-ViewModel) + Coroutines for highly concurrent background I/O operations.
- **Networking:** Custom UDP Datagram socket implementations with raw byte-level packet assembly (`Packetizer`, `FragmentAssembler`).
- **Audio Engine:** Low-level `AudioTrack` and `AudioRecord` API utilization with `USAGE_MEDIA` overrides.
- **AI/Speech:** Integrated SherpaONNX for completely on-device, offline Text-To-Speech (TTS) and speech recognition.

---

## ⚙️ How to Build and Run

1. Clone the repository to your local machine.
2. Open the **`MyApplication`** folder in Android Studio.
3. Sync Gradle.
4. Build and run on a physical Android device (Emulators do not support Wi-Fi Direct hardware).
5. For testing, install the application on at least **two** physical devices to form a mesh.

*Note: You must grant Nearby Devices, Microphone, and Location permissions for the mesh and walkie-talkie to function.*

---

<div align="center">
  <i>Built with ❤️ for the Smart India Hackathon.</i>
</div>
