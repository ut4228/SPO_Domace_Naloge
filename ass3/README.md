# SIC/XE Assembler AST Pretty Printer

This Java project parses SIC/XE assembly source, builds an abstract syntax tree (AST), and now assembles it into raw bytes and standard SIC/XE object text. It covers comments, directives, storage directives (BYTE/WORD/RESB/RESW) and instructions of formats 1–4.

## Usage

### Bash (run.sh)

```bash
./run.sh programs/print.asm
```

### Windows PowerShell

```powershell
$files = Get-ChildItem -Recurse src\main\java\*.java
javac -d out $files
java -cp out assembler.Main programs/print.asm
```

Replace the path with your source file. The assembler writes:

- `*.obj` – SIC/XE object text (H/T/E records, 30-byte text records).
- `*.bin` – raw bytes (uninitialized storage padded with zeros).
- `*.log` – pretty AST, symbol table, object text, and a hex dump of raw bytes.

The console prints the locations of the generated files. If your source has undefined symbols (e.g., external routines), resolution will fail until they are provided.

## Project layout

- `src/main/java/assembler/*.java` – AST nodes, parser, mnemonic classes, CLI.
- `sample.sic` – Example program to try.

## Extending

- Add more mnemonics or operand rules in `build_mnemonic_table`.
- Enhance operand parsing (e.g., literals, expressions) by expanding helper methods in `Parser`.
- Implement code generation or symbol resolution on top of the AST nodes.
