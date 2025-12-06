import argparse
import pathlib
from typing import Dict, List, Optional, Tuple

OPCODES = {
    "ADD": 0x18,
    "ADDF": 0x58,
    "ADDR": 0x90,
    "AND": 0x40,
    "COMP": 0x28,
    "COMPF": 0x88,
    "COMPR": 0xA0,
    "DIV": 0x24,
    "DIVF": 0x64,
    "DIVR": 0x9C,
    "J": 0x3C,
    "JEQ": 0x30,
    "JGT": 0x34,
    "JLT": 0x38,
    "JSUB": 0x48,
    "LDA": 0x00,
    "LDB": 0x68,
    "LDCH": 0x50,
    "LDF": 0x70,
    "LDL": 0x08,
    "LDS": 0x6C,
    "LDT": 0x74,
    "LDX": 0x04,
    "MUL": 0x20,
    "MULF": 0x60,
    "MULR": 0x98,
    "OR": 0x44,
    "RD": 0xD8,
    "RMO": 0xAC,
    "RSUB": 0x4C,
    "SHIFTL": 0xA4,
    "SHIFTR": 0xA8,
    "STA": 0x0C,
    "STB": 0x78,
    "STCH": 0x54,
    "STF": 0x80,
    "STL": 0x14,
    "STS": 0x7C,
    "STSW": 0xE8,
    "STT": 0x84,
    "STX": 0x10,
    "SUB": 0x1C,
    "SUBF": 0x5C,
    "SUBR": 0x94,
    "TD": 0xE0,
    "TIX": 0x2C,
    "TIXR": 0xB8,
    "WD": 0xDC,
    "CLEAR": 0xB4,
}

FORMAT1 = {"FIX", "FLOAT", "NORM", "SIO", "HIO", "TIO"}
FORMAT2 = {
    "ADDR",
    "SUBR",
    "MULR",
    "DIVR",
    "COMPR",
    "CLEAR",
    "TIXR",
    "SHIFTL",
    "SHIFTR",
    "RMO",
}

REGISTERS = {
    "A": 0,
    "X": 1,
    "L": 2,
    "B": 3,
    "S": 4,
    "T": 5,
    "F": 6,
    "PC": 8,
    "SW": 9,
}

class AsmLine:
    def __init__(self, label: Optional[str], opcode: str, operand: str, loc: int):
        self.label = label
        self.opcode = opcode
        self.operand = operand
        self.loc = loc


def strip_comment(line: str) -> str:
    stripped = line.rstrip('\n')
    if not stripped:
        return ''
    lstripped = stripped.lstrip()
    if lstripped.startswith('.'):
        return ''
    result_chars = []
    in_quote = False
    i = 0
    while i < len(stripped):
        ch = stripped[i]
        if ch == "'":
            in_quote = not in_quote
            result_chars.append(ch)
            i += 1
            continue
        if not in_quote and ch == '.' and (i == 0 or stripped[i - 1].isspace()):
            break
        result_chars.append(ch)
        i += 1
    return ''.join(result_chars).rstrip()


def parse_line(line: str) -> Tuple[Optional[str], Optional[str], Optional[str]]:
    cleaned = strip_comment(line)
    if not cleaned:
        return None, None, None
    tokens = cleaned.split()
    if not tokens:
        return None, None, None
    label = None
    opcode = None
    operand = ''
    if cleaned[0].isspace():
        opcode = tokens[0]
        remainder = cleaned[cleaned.index(opcode) + len(opcode):].strip()
        operand = remainder
    else:
        label = tokens[0]
        if len(tokens) > 1:
            opcode = tokens[1]
            start = cleaned.index(opcode, cleaned.index(label) + len(label))
            operand = cleaned[start + len(opcode):].strip()
    if opcode:
        opcode = opcode.upper()
    operand = operand.replace('\t', ' ').strip()
    return label, opcode, operand


def parse_number(token: str, current_loc: int, symbols: Dict[str, int]) -> int:
    token = token.strip()
    if token == '*':
        return current_loc
    if token.startswith('-'):
        return -parse_number(token[1:], current_loc, symbols)
    if token.startswith('0X') or token.startswith('0x'):
        return int(token, 16)
    if token.isdigit():
        return int(token)
    if token in symbols:
        return symbols[token]
    raise ValueError(f"Unknown symbol {token}")


def eval_expression(expr: str, current_loc: int, symbols: Dict[str, int]) -> int:
    expr = expr.replace('+', ' + ').replace('-', ' - ')
    tokens = expr.split()
    total = 0
    sign = 1
    for tok in tokens:
        if tok == '+':
            sign = 1
        elif tok == '-':
            sign = -1
        else:
            total += sign * parse_number(tok, current_loc, symbols)
    return total


def byte_length(opcode: str, operand: str) -> int:
    if opcode == 'WORD':
        return 3
    if opcode == 'RESW':
        return 3 * int(operand)
    if opcode == 'RESB':
        return int(operand)
    if opcode == 'BYTE':
        opnd = operand.strip()
        if opnd.startswith("C'") and opnd.endswith("'"):
            return len(opnd[2:-1])
        if opnd.startswith("X'") and opnd.endswith("'"):
            return len(opnd[2:-1]) // 2
        return 1
    return 0


def instruction_size(opcode: str) -> int:
    extended = opcode.startswith('+')
    base = opcode.lstrip('+')
    if base in FORMAT1:
        return 1
    if base in FORMAT2:
        return 2
    return 4 if extended else 3


def assemble(source: pathlib.Path, output: pathlib.Path) -> None:
    lines = source.read_text(encoding='utf-8').splitlines()
    symbols: Dict[str, int] = {}
    intermediate: List[AsmLine] = []
    loc = 0
    start_addr = 0
    program_name = source.stem.upper()[:6]
    end_operand = None
    pending_label: Optional[str] = None

    for raw in lines:
        label, opcode, operand = parse_line(raw)
        if opcode is None:
            if label:
                pending_label = label
            continue
        if label is None and pending_label:
            label = pending_label
            pending_label = None
        elif label is not None:
            pending_label = None
        if opcode == 'START':
            start_addr = int(operand, 16) if operand.startswith('0X') else int(operand)
            loc = start_addr
            if label:
                program_name = label[:6].upper()
                symbols[label] = loc
            continue
        if opcode == 'END':
            end_operand = operand if operand else None
            break
        if label:
            if label in symbols:
                raise ValueError(f"Duplicate label {label}")
            symbols[label] = loc
        if opcode == 'EQU':
            if not label:
                raise ValueError("EQU requires a label")
            symbols[label] = eval_expression(operand, loc, symbols)
            continue
        if opcode in {'WORD', 'RESW', 'RESB', 'BYTE'}:
            size = byte_length(opcode, operand)
            intermediate.append(AsmLine(label, opcode, operand, loc))
            loc += size
            continue
        size = instruction_size(opcode)
        intermediate.append(AsmLine(label, opcode, operand, loc))
        loc += size

    program_length = loc - start_addr
    entry_point = symbols.get(end_operand, start_addr)

    text_records: List[Tuple[int, bytearray]] = []
    current_start = None
    current_bytes: Optional[bytearray] = None

    def flush_record() -> None:
        nonlocal current_start, current_bytes
        if current_bytes:
            text_records.append((current_start, current_bytes))
            current_bytes = None
            current_start = None

    for line in intermediate:
        opcode = line.opcode
        operand = line.operand
        if opcode in {'RESW', 'RESB'}:
            flush_record()
            continue
        if opcode == 'WORD':
            value = eval_expression(operand, line.loc, symbols) & 0xFFFFFF
            obj_hex = f"{value:06X}"
        elif opcode == 'BYTE':
            operand = operand.strip()
            if operand.startswith("C'") and operand.endswith("'"):
                obj_hex = ''.join(f"{ord(c):02X}" for c in operand[2:-1])
            elif operand.startswith("X'") and operand.endswith("'"):
                obj_hex = operand[2:-1]
            else:
                value = eval_expression(operand, line.loc, symbols)
                if not 0 <= value <= 0xFF:
                    raise ValueError("BYTE immediate value must fit in one byte")
                obj_hex = f"{value & 0xFF:02X}"
        else:
            obj_hex = assemble_instruction(line, symbols)
        obj_bytes = bytearray.fromhex(obj_hex)
        if current_bytes is None:
            current_start = line.loc
            current_bytes = bytearray()
        if len(current_bytes) + len(obj_bytes) > 30:
            flush_record()
            current_start = line.loc
            current_bytes = bytearray()
        current_bytes.extend(obj_bytes)
    flush_record()

    with output.open('w', encoding='utf-8') as f:
        f.write(f"H^{program_name:<6}^{start_addr:06X}^{program_length:06X}\n")
        for start, data in text_records:
            hexdata = ''.join(f"{b:02X}" for b in data)
            f.write(f"T^{start:06X}^{len(data):02X}^{hexdata}\n")
        f.write(f"E^{entry_point:06X}\n")


def assemble_instruction(line: AsmLine, symbols: Dict[str, int]) -> str:
    opcode = line.opcode
    operand = line.operand.replace(' ', '')
    extended = opcode.startswith('+')
    base = opcode.lstrip('+')
    if base in FORMAT2:
        return assemble_format2(base, operand)
    if base in FORMAT1:
        raise ValueError("Format 1 opcodes not implemented")
    if base == 'RSUB' and operand == '':
        return f"{(OPCODES[base] | 0x03):02X}0000"
    code = OPCODES.get(base)
    if code is None:
        raise ValueError(f"Unknown opcode {base}")
    n = 1
    i = 1
    x = 0
    b = 0
    p = 0
    e = 1 if extended else 0
    operand_field = operand
    target_addr = 0
    immediate_value: Optional[int] = None
    if operand_field:
        if operand_field.startswith('#'):
            n = 0
            i = 1
            operand_field = operand_field[1:]
            try:
                immediate_value = int(operand_field, 0)
            except ValueError:
                target_addr = symbols.get(operand_field, 0)
        elif operand_field.startswith('@'):
            n = 1
            i = 0
            operand_field = operand_field[1:]
        if operand_field.endswith(',X'):
            x = 1
            operand_field = operand_field[:-2]
    if immediate_value is not None and not extended:
        disp = immediate_value & 0xFFF
    else:
        if operand_field:
            if operand_field.startswith('0X') or operand_field.startswith('0x'):
                target_addr = int(operand_field, 16)
            elif operand_field.isdigit():
                target_addr = int(operand_field)
            else:
                if operand_field not in symbols:
                    raise ValueError(f"Undefined symbol {operand_field}")
                target_addr = symbols[operand_field]
        if extended:
            disp = target_addr & 0xFFFFF
        else:
            next_loc = line.loc + 3
            offset = target_addr - next_loc
            if -2048 <= offset <= 2047:
                p = 1
                disp = offset & 0xFFF
            else:
                raise ValueError("Displacement out of range (BASE not supported)")
    opcode_val = (code & 0xFC) | (n << 1) | i
    xbpe = (x << 3) | (b << 2) | (p << 1) | e
    if extended:
        return f"{opcode_val:02X}{xbpe:01X}{disp:05X}"
    return f"{opcode_val:02X}{xbpe:01X}{disp:03X}"


def assemble_format2(opcode: str, operand: str) -> str:
    code = OPCODES[opcode]
    if not operand:
        r1 = 0
        r2 = 0
    else:
        parts = operand.split(',')
        if len(parts) == 1:
            r1 = REGISTERS[parts[0]]
            r2 = 0
        else:
            r1 = REGISTERS[parts[0]]
            second = parts[1]
            if second.isdigit():
                r2 = int(second)
            else:
                r2 = REGISTERS[second]
    return f"{code:02X}{r1:01X}{r2:01X}"


def main() -> None:
    parser = argparse.ArgumentParser(description="Minimal SIC/XE assembler")
    parser.add_argument('source', type=pathlib.Path)
    parser.add_argument('-o', '--output', type=pathlib.Path)
    args = parser.parse_args()
    output = args.output if args.output else args.source.with_suffix('.obj')
    assemble(args.source, output)


if __name__ == '__main__':
    main()
