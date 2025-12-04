public class Registers {
    int A, X, L, B, S, T, PC, SW;
    long F;

    public Registers() {
        A = X = L = B = S = T = PC = SW = 0;
        F = 0;
    }

    public int getReg(int reg) {
        switch (reg) {
            case 0:
                return A;
            case 1:
                return X;
            case 2:
                return L;
            case 3:
                return B;
            case 4:
                return S;
            case 5:
                return T;
            case 6:
                return (int) F;
            case 8:
                return PC;
            case 9:
                return SW;
            default:
                throw new IllegalArgumentException("Bad register");
        }
    }

    public void setReg(int reg, int val) {
        val &= 0xFFFFFF;
        switch (reg) {
            case 0:
                A = val;
                break;
            case 1:
                X = val;
                break;
            case 2:
                L = val;
                break;
            case 3:
                B = val;
                break;
            case 4:
                S = val;
                break;
            case 5:
                T = val;
                break;
            case 6:
                F = val;
                break;
            case 8:
                PC = val;
                break;
            case 9:
                SW = val;
                break;
        }
    }
}
