# SIC/XE Simulator

Simulator implements the SIC/XE machine used on the SPO course. It provides register, memory and device models, supports SIC/XE addressing modes and core integer instruction set, and offers a simple command-line interface for loading and executing object files.

## Features

- Full set of general-purpose registers (A, X, L, B, S, T, PC, SW) with byte/word/float memory access helpers
- 1 MiB memory with byte, word and floating-point accessors
- Device table with standard input/output/error mapped to device IDs 0, 1 and 2 and file-backed devices for the rest
- Instruction fetch/decode/execute loop with support for Formats 1–4 (except floating-point/system opcodes)
- Implemented instructions: load/store, integer arithmetic, bitwise logic, comparisons, jumps, register operations, and basic device I/O (RD/WD/TD)
- Execution control with `step`, `start`, `stop`, and adjustable speed timer
- Absolute loader for SIC/XE object files (`.obj`)
- Interactive CLI (`Simulator`) for inspecting registers, stepping, dumping memory, and managing execution

## Prerequisites

- Java Development Kit 17 (or newer)
- Bash-compatible shell (for `run.sh`)

## Build and Run

From the repository root:

```bash
./run.sh [program.obj]
```

Without arguments the simulator starts at an empty machine state. When a path to an object file is provided, it is loaded automatically before the prompt appears.

## CLI Commands

- `help` – list available commands
- `load <path>` – load another object file (resets memory and registers)
- `regs` – show register values and condition code
- `status` – show PC, run state, execution speed, and condition code
- `pc` – print the current program counter
- `step` – execute a single instruction
- `run [n]` – execute _n_ instructions (default 1)
- `start` / `stop` – begin or halt automatic execution
- `speed <kHz>` – set automatic execution speed
- `mem [addr] [len]` – dump memory in bytes; falls back to the most recent range when omitted
- `memw [addr] [count]` – dump SIC/XE words showing both hex and signed decimal
- `vars [count]` – dump the last `count` words of the currently loaded program (data area)
- `namevars <names...>` – label the addresses shown by the most recent `vars` dump for easier tracking
- `label <addr> <name>` – assign a label to any absolute address; `unlabel <addr|name>` removes it and `labels` lists them all
- `clear` – reset registers, memory, labels, and load metadata
- `quit` / `exit` – leave the simulator

All numeric arguments accept decimal, hexadecimal (`0x` prefix), or any other format supported by `Integer.decode`.

## Notes

- Floating-point instructions (ADDF, MULF, etc.) and privileged opcodes (HIO/SIO/TIO) are treated as unimplemented.
- Device IDs above 2 are mapped to per-device files named `deviceNNN.dat` in the working directory.
- Error conditions (invalid opcodes/addressing, divide by zero, missing devices) are reported on standard error.
