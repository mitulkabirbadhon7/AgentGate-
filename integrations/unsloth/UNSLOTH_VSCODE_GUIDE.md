# Unsloth, React Bits & MotionSites — Developer Guide & Implementation

## 🌟 Executive Summary

This document explains what **React Bits**, **MotionSites.ai**, and **Unsloth (and its IDE extensions)** are, how they are installed, and how they have been implemented across our projects.

---

## 1. ⚛️ React Bits (`reactbits.dev`)

### What It Is
**React Bits** is an open-source, modular animated component library built with **React**, **TypeScript**, **Tailwind CSS**, and **Framer Motion**.
Instead of bloated, rigid monolithic npm packages, React Bits follows the **shadcn/ui model**: individual, customizable animated components that live directly in your codebase (`src/components/ui/` and `src/components/animations/`).

### How It Is Installed
Components are added modularly. We have created and installed the core React Bits suite in `@european-dining/frontend`:
- `SpotlightCard.tsx`: Interactive cursor-tracking radial spotlight card.
- `BlurText.tsx`: Luxury staggered word/letter reveal using spring physics (`cubic-bezier(0.16, 1, 0.3, 1)`).
- `DecryptedText.tsx`: Scrambled/decrypted cyber-text transition.
- `MagneticButton.tsx`: Interactive magnetic attraction button following cursor coordinates.

In **AgentGate**, we implemented the React Bits spotlight effect into the Thymeleaf admin dashboard (`style.css` and `fragments.html`) so cards dynamically glow as the cursor moves over them.

---

## 2. 🌊 MotionSites.ai (`motionsites.ai`)

### What It Is
**MotionSites.ai** is a premium design and AI motion prompt library for **Next.js**, **Tailwind**, and **Framer Motion**. It provides bespoke motion architectures, fluid fluid transitions, and ambient lighting to eliminate generic "AI slop" aesthetics and achieve award-winning, agency-grade digital experiences.

### How It Is Implemented
- `AuroraBackground.tsx`: Multi-layered atmospheric ambient glow using smooth continuous cubic-bezier animations tailored to bespoke European brand palettes (Espresso, Bordeaux, and Tuscan Terracotta).
- Integrated into the hero headers and showcase cards in `page.tsx`, `bistro/page.tsx`, and `pizzeria/page.tsx`.

---

## 3. 🦥 Unsloth & Unsloth Extension (`unsloth.ai`)

### What It Is
**Unsloth** is an ultra-fast, memory-efficient framework for fine-tuning and running open-weight Large Language Models (LLMs) like **Llama 3.2**, **Llama 3.1**, **Qwen 2.5**, **Mistral**, and **Gemma 2**.
- **Speed:** 2x–5x faster training compared to standard Hugging Face implementations.
- **Memory:** Up to 80% reduced VRAM consumption using custom OpenAI Triton kernels.

### What is the "Unsloth Extension"?
When developers refer to an "Unsloth extension", they are referring to one of three workflows:

1. **The Google Colab VS Code Extension (`googlecolab.colab-vscode`):**
   - Fine-tuning with Unsloth requires NVIDIA GPUs (T4, A100, RTX 3090/4090).
   - The **Google Colab extension in VS Code** allows you to connect your local IDE workspace directly to Google Colab's free cloud GPU runtimes.
   - You can edit and run your Unsloth Python scripts and Jupyter notebooks directly inside VS Code without ever touching a web browser.

2. **Unsloth Studio / Desktop Local Inference Server:**
   - Unsloth Studio provides an OpenAI-compatible local API server (`http://localhost:8000/v1`).
   - You can point coding agent extensions in VS Code (such as **Continue**, **GitHub Copilot**, or **Roo Code**) to this endpoint to use your fine-tuned local models.

3. **AgentGate Zero-Trust Governance Integration:**
   - In `integrations/unsloth/`:
     - `unsloth_finetune_pipeline.py`: Ready-to-run 4-bit LoRA fine-tuning and GGUF export script.
     - `unsloth_server_bridge.py`: Local OpenAI-compatible server bridge routing tool calls through AgentGate's `/mcp` zero-trust perimeter.

---

## 4. 🛠️ VS Code Setup Instructions for Unsloth

1. Open VS Code Extensions (`Ctrl+Shift+X` / `Cmd+Shift+X`).
2. Search and install:
   - **Google Colab** (by Google): Connects VS Code to free cloud GPU kernels.
   - **Continue** (by Continue): Connects local models running via Unsloth to VS Code chat and autocomplete.
3. Configure `.vscode/settings.json`:
```json
{
  "continue.customCommands": [
    {
      "name": "unsloth-local",
      "prompt": "Run code generation via Unsloth local engine"
    }
  ]
}
```
4. Run the local Unsloth bridge:
```bash
python integrations/unsloth/unsloth_server_bridge.py
```
