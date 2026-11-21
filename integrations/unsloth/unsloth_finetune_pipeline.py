"""
Unsloth Fine-Tuning Pipeline for AgentGate & Autonomous Agents
============================================================
Optimized 2x-5x faster training with up to 80% reduced VRAM using Unsloth FastLanguageModel.
Supports Llama 3.2 (1B/3B), Llama 3.1 (8B), Qwen 2.5 (7B/14B), and Mistral NeMo.
"""

import os
import torch

def setup_unsloth_training(
    model_name: str = "unsloth/Llama-3.2-3B-Instruct",
    max_seq_length: int = 2048,
    load_in_4bit: bool = True,
    lora_rank: int = 16,
    lora_alpha: int = 16,
    output_dir: str = "outputs",
):
    """
    Initializes Unsloth FastLanguageModel and PEFT LoRA adapters.
    """
    try:
        from unsloth import FastLanguageModel
        from trl import SFTTrainer
        from transformers import TrainingArguments
        from datasets import load_dataset
    except ImportError:
        print("[!] Unsloth dependencies not detected in local environment.")
        print("[!] Install using:")
        print("    pip install --upgrade --no-cache-dir 'unsloth[colab-new] @ git+https://github.com/unslothai/unsloth.git'")
        print("    pip install --no-deps trl peft accelerate bitsandbytes")
        return None

    print(f"[*] Loading model {model_name} with Unsloth 4-bit quantization...")
    model, tokenizer = FastLanguageModel.from_pretrained(
        model_name=model_name,
        max_seq_length=max_seq_length,
        dtype=None, # Auto-detect Float16 / Bfloat16
        load_in_4bit=load_in_4bit,
    )

    print("[*] Attaching parameter-efficient LoRA adapters...")
    model = FastLanguageModel.get_peft_model(
        model,
        r=lora_rank,
        target_modules=[
            "q_proj", "k_proj", "v_proj", "o_proj",
            "gate_proj", "up_proj", "down_proj",
        ],
        lora_alpha=lora_alpha,
        lora_dropout=0, # Unsloth supports 0 dropout for optimized kernel execution
        bias="none",
        use_gradient_checkpointing="unsloth", # 30% longer context / less VRAM
        random_state=3407,
    )

    return model, tokenizer

def export_to_gguf(model, tokenizer, output_dir: str = "agent_model_gguf"):
    """
    Exports the fine-tuned model directly to 4-bit / 8-bit GGUF format
    for immediate local inference with Ollama, llama.cpp, or Unsloth Desktop.
    """
    from unsloth import FastLanguageModel
    print(f"[*] Exporting model to GGUF format for local deployment: {output_dir}")
    model.save_pretrained_gguf(
        output_dir,
        tokenizer,
        quantization_method="q4_k_m" # Optimal quality/size balance
    )
    print(f"[✓] GGUF exported to {output_dir}. Deploy locally or hook into AgentGate.")

if __name__ == "__main__":
    print("=" * 60)
    print("Unsloth Autonomous Agent Fine-Tuning Pipeline")
    print("=" * 60)
    print("Run this script in a GPU-enabled environment (e.g. Google Colab via VS Code extension).")
