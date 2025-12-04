public class Opcode {
    // Operacijske kode SIC/XE

    // Format 3/4
    public static final int ADD = 0x18; // A <- (A) + (m..m+2)
    public static final int ADDF = 0x58; // F <- (F) + (m..m+5)
    public static final int AND = 0x40; // A <- (A) & (m..m+2)
    public static final int COMP = 0x28; // (A) : (m..m+2)
    public static final int COMPF = 0x88; // (F) : (m..m+5)
    public static final int DIV = 0x24; // A <- (A) / (m..m+2)
    public static final int DIVF = 0x64; // F <- (F) / (m..m+2)
    public static final int MUL = 0x20; // A <- (A) * (m..m+2)
    public static final int MULF = 0x60; // F <- (F) * (m..m+5)
    public static final int OR = 0x44; // A <- (A) | (m..m+2)
    public static final int RD = 0xD8; // A <- (m); (A)
    public static final int WD = 0xDC; // write(A)
    public static final int SUB = 0x1C; // A <- (A) - (m..m+2)
    public static final int SUBF = 0x5C; // F <- (F) - (m..m+5)
    public static final int TIX = 0x2C; // X <- (X) + 1; (X) : (m..m+2)
    public static final int LDA = 0x00; // A <- (m..m+2)
    public static final int LDB = 0x68; // B <- (m..m+2)
    public static final int LDCH = 0x50; // A[low] <- (m)
    public static final int LDF = 0x70; // F <- (m..m+5)
    public static final int LDL = 0x08; // L <- (m..m+2)
    public static final int LDS = 0x6C; // S <- (m..m+2)
    public static final int LDT = 0x74; // T <- (m..m+2)
    public static final int LDX = 0x04; // X <- (m..m+2)
    public static final int STA = 0x0C; // m..m+2 <- (A)
    public static final int STB = 0x78; // m..m+2 <- (B)
    public static final int STCH = 0x54; // m <- (A)[low]
    public static final int STF = 0x80; // m..m+5 <- (F)
    public static final int STI = 0xD4; // time <- (m..m+2)
    public static final int STL = 0x14; // m..m+2 <- (L)
    public static final int STS = 0x7C; // m..m+2 <- (S)
    public static final int STSW = 0xE8; // m..m+2 <- (SW)
    public static final int STT = 0x84; // m..m+2 <- (T)
    public static final int STX = 0x10; // m..m+2 <- (X)
    public static final int SSK = 0xEC; // m..m+2 <- (A)
    public static final int J = 0x3C; // PC <- m
    public static final int JEQ = 0x30; // PC <- m if CC = =
    public static final int JGT = 0x34; // PC <- m if CC > =
    public static final int JLT = 0x38; // PC <- m if CC < =
    public static final int JSUB = 0x48; // L <- (PC); PC <- m
    public static final int TD = 0xE0; // testdev(m)
    public static final int TIO = 0xF8; // test(A)

    // Format 2
    public static final int TIXR = 0xB8; // X <- (X) + 1; (X) : (r1)
    public static final int SUBR = 0x94; // r2 <- (r2) - (r1)
    public static final int RMO = 0xAC; // r2 <- (r1)
    public static final int MULR = 0x98; // r2 <- (r2) * (r1)
    public static final int DIVR = 0x9C; // r2 <- (r2) / (r1)
    public static final int COMPR = 0xA0; // (r1) : (r2)
    public static final int ADDR = 0x90; // r2 <- (r2) + (r1)
    public static final int CLEAR = 0xB4; // r <- 0
    public static final int SHIFTL = 0xA4; // (r1) <- (r1) << n
    public static final int SHIFTR = 0xA8; // (r1) <- (r1) >> n
    public static final int SVC = 0xB0; // interrupt(n)

    // Format 1
    public static final int RSUB = 0x4C; // PC <- (L)
    public static final int FLOAT = 0xC0; // F <- float(A)
    public static final int FIX = 0xC4; // A <- fix(F)
    public static final int HIO = 0xF4; // halt(A)
    public static final int NORM = 0xC8; // F <- norm(F)
    public static final int SIO = 0xF0; // start(A, X)

    // Pomocne metode za določanje formata operacijske kode
    public static int getFormat(int opcode) {
        // Format 1 (1 bajt)
        if (opcode == RSUB || opcode == FLOAT || opcode == FIX ||
                opcode == HIO || opcode == NORM || opcode == SIO) {
            return 1;
        }

        // Format 2 (2 bajta)
        if (opcode == TIXR || opcode == SUBR || opcode == RMO ||
                opcode == MULR || opcode == DIVR || opcode == COMPR ||
                opcode == ADDR || opcode == CLEAR || opcode == SHIFTL ||
                opcode == SHIFTR || opcode == SVC) {
            return 2;
        }

        // Format 3/4 (3 ali 4 bajte - odvisno od e-bita)
        return 3; // Privzeto format 3, lahko se razširi na format 4
    }

    // Vrne ime operacijske kode (za debug/izpis)
    public static String getName(int opcode) {
        switch (opcode) {
            case ADD:
                return "ADD";
            case ADDF:
                return "ADDF";
            case ADDR:
                return "ADDR";
            case AND:
                return "AND";
            case CLEAR:
                return "CLEAR";
            case COMP:
                return "COMP";
            case COMPF:
                return "COMPF";
            case COMPR:
                return "COMPR";
            case DIV:
                return "DIV";
            case DIVF:
                return "DIVF";
            case DIVR:
                return "DIVR";
            case FIX:
                return "FIX";
            case FLOAT:
                return "FLOAT";
            case HIO:
                return "HIO";
            case J:
                return "J";
            case JEQ:
                return "JEQ";
            case JGT:
                return "JGT";
            case JLT:
                return "JLT";
            case JSUB:
                return "JSUB";
            case LDA:
                return "LDA";
            case LDB:
                return "LDB";
            case LDCH:
                return "LDCH";
            case LDF:
                return "LDF";
            case LDL:
                return "LDL";
            case LDS:
                return "LDS";
            case LDT:
                return "LDT";
            case LDX:
                return "LDX";
            case MUL:
                return "MUL";
            case MULF:
                return "MULF";
            case MULR:
                return "MULR";
            case NORM:
                return "NORM";
            case OR:
                return "OR";
            case RD:
                return "RD";
            case WD:
                return "WD";
            case RMO:
                return "RMO";
            case RSUB:
                return "RSUB";
            case SHIFTL:
                return "SHIFTL";
            case SHIFTR:
                return "SHIFTR";
            case SIO:
                return "SIO";
            case SSK:
                return "SSK";
            case STA:
                return "STA";
            case STB:
                return "STB";
            case STCH:
                return "STCH";
            case STF:
                return "STF";
            case STI:
                return "STI";
            case STL:
                return "STL";
            case STS:
                return "STS";
            case STSW:
                return "STSW";
            case STT:
                return "STT";
            case STX:
                return "STX";
            case SUB:
                return "SUB";
            case SUBF:
                return "SUBF";
            case SUBR:
                return "SUBR";
            case SVC:
                return "SVC";
            case TD:
                return "TD";
            case TIO:
                return "TIO";
            case TIX:
                return "TIX";
            case TIXR:
                return "TIXR";
            default:
                return "UNKNOWN";
        }
    }
}
