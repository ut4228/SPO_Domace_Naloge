public class Machine {
    Registers regs;
    Memory mem;
    Device[] devices;

    // Simulacija procesorjeve ure
    private java.util.Timer timer;
    private boolean running = false;
    // Hitrost v kHz (koliko tisoč ukazov na sekundo želimo)
    private int speedKHz = 1; // privzeto 1 kHz
    // Koliko ukazov izvedemo ob vsakem tiku časovnika (trik za pohitritev)
    private int opsPerTick = 10;

    public Machine(int memSize) {
        regs = new Registers();
        mem = new Memory(memSize);
        devices = new Device[256];

        // Inicializiraj standardne naprave
        devices[0] = new InputDevice(0, System.in); // Standardni vhod
        devices[1] = new OutputDevice(1, System.out); // Standardni izhod
        devices[2] = new OutputDevice(2, System.err); // Standardni izhod za napake

        // Naprave 3-255 bodo FileDevice ali ostanejo null
    }

    void setDevice(int num, Device device) {
        if (num < 0 || num >= devices.length) {
            throw new IllegalArgumentException("Invalid device number: " + num);
        }
        devices[num] = device;
    }

    Device getDevice(int num) {
        if (num < 0 || num >= devices.length) {
            throw new IllegalArgumentException("Invalid device number: " + num);
        }
        return devices[num];
    }

    // =====================
    // Obravnava težav
    // =====================

    // Ni ravno težava stroja, bolj težava programerja. :)
    void notImplemented(String mnemonic) {
        System.err.println("Not implemented: " + mnemonic);
    }

    // Izvajalnik je naletel na operacijsko kodo ukaza, ki ni veljavna.
    void invalidOpcode(int opcode) {
        System.err.println("Invalid opcode: 0x" + Integer.toHexString(opcode));
    }

    // Neveljavno naslavljanje.
    void invalidAddressing() {
        System.err.println("Invalid addressing mode");
    }

    // =====================
    // Izvajalnik
    // =====================

    // Naloži in vrne en bajt z naslova v PC ter PC poveča za ena
    int fetch() {
        int pc = regs.PC & 0xFFFFF; // 20-bitni PC
        int b = mem.getByte(pc);
        regs.PC = (pc + 1) & 0xFFFFF; // poveča PC, ostane v 20-bitnem prostoru
        return b;
    }

    // Dekodiraj ukaz in operande na naslovu PC ter ga izvedi
    void execute() {
        // Preberemo prvi bajt (opcode z 8 biti)
        int opByte = fetch();
        int opcode = opByte & 0xFC; // zgornjih 6 bitov je dejanski opcode
        int format = Opcode.getFormat(opcode);

        switch (format) {
            case 1: {
                boolean done = execF1(opcode);
                // format 1 je 1 bajt; PC je že povečan v fetch()
                if (done) {
                    // lahko obravnavamo končanje
                }
                break;
            }
            case 2: {
                // Format 2 ima še 1 bajt operandov (r1/r2 ali r/n)
                int operand = fetch();
                boolean done = execF2(opcode, operand);
                if (done) {
                    // končanje
                }
                break;
            }
            case 3: // pade skozi na 4, operand branje je enako, e-bit določa dolžino
            default: {
                // Format 3/4: potrebujemo še 2 bajta za operand (skupno 3 bajte),
                // če je e-bit nastavljen, še dodatni bajt (format 4)
                int b2 = fetch();
                int b3 = fetch();
                int ni = (opByte & 0x03); // zadnja 2 bita prvega bajta (n,i)
                int xbpe = (b2 >> 4) & 0x0F; // X,B,P,E iz drugega bajta
                boolean e = (xbpe & 0x1) == 1;
                int dispOrAddr;
                if (e) {
                    // format 4: še en bajt
                    int b4 = fetch();
                    dispOrAddr = ((b2 & 0x0F) << 16) | (b3 << 8) | b4; // 20-bitni naslov
                } else {
                    // format 3: 12-bitni displacement
                    dispOrAddr = ((b2 & 0x0F) << 8) | b3; // 12-bit
                }
                boolean done = execSICF3F4(opcode, ni, xbpe, dispOrAddr);
                if (done) {
                    // končanje
                }
                break;
            }
        }
    }

    // Izvajalniki posameznih formatov

    // Format 1
    boolean execF1(int opcode) {
        // Primer: RSUB
        if (opcode == Opcode.RSUB) {
            regs.PC = regs.L; // povratek
            return true; // končaj trenutno izvajanje
        }
        notImplemented(Opcode.getName(opcode));
        return false;
    }

    // Format 2
    boolean execF2(int opcode, int operand) {
        // operand: zgornjih 4 bitov r1, spodnjih 4 bitov r2 ali n
        int r1 = (operand >> 4) & 0x0F;
        int r2 = operand & 0x0F;
        switch (opcode) {
            case Opcode.CLEAR:
                regs.setReg(r1, 0);
                return false;
            case Opcode.COMPR: {
                int v1 = regs.getReg(r1);
                int v2 = regs.getReg(r2);
                // nastavitev CC v SW: 0x00 <, 0x40 =, 0x80 > (konvencionalno)
                int cc;
                if (v1 < v2)
                    cc = 0x00;
                else if (v1 == v2)
                    cc = 0x40;
                else
                    cc = 0x80;
                regs.SW = cc;
                return false;
            }
            default:
                notImplemented(Opcode.getName(opcode));
                return false;
        }
    }

    // Format 3/4 (SIC/XE)
    boolean execSICF3F4(int opcode, int ni, int xbpe, int dispOrAddr) {
        switch (opcode) {
            case Opcode.LDA: {
                int val = loadByAddressing(ni, xbpe, dispOrAddr);
                regs.A = val;
                return false;
            }
            case Opcode.LDX: {
                int val = loadByAddressing(ni, xbpe, dispOrAddr);
                regs.X = val;
                return false;
            }
            case Opcode.LDL: {
                int val = loadByAddressing(ni, xbpe, dispOrAddr);
                regs.L = val;
                return false;
            }
            case Opcode.STA: {
                int addr = effectiveAddress(ni, xbpe, dispOrAddr);
                mem.setWord(addr, regs.A);
                return false;
            }
            case Opcode.STX: {
                int addr = effectiveAddress(ni, xbpe, dispOrAddr);
                mem.setWord(addr, regs.X);
                return false;
            }
            case Opcode.STL: {
                int addr = effectiveAddress(ni, xbpe, dispOrAddr);
                mem.setWord(addr, regs.L);
                return false;
            }
            case Opcode.LDCH: {
                int addr = effectiveAddress(ni, xbpe, dispOrAddr);
                int b = mem.getByte(addr);
                regs.A = (regs.A & 0xFFFF00) | (b & 0xFF);
                return false;
            }
            case Opcode.STCH: {
                int addr = effectiveAddress(ni, xbpe, dispOrAddr);
                mem.setByte(addr, regs.A & 0xFF);
                return false;
            }
            case Opcode.J: {
                int addr = effectiveAddress(ni, xbpe, dispOrAddr);
                regs.PC = addr;
                return false;
            }
            case Opcode.JSUB: {
                int addr = effectiveAddress(ni, xbpe, dispOrAddr);
                regs.L = regs.PC;
                regs.PC = addr;
                return false;
            }
            case Opcode.TD: { // test device
                int devNum = effectiveAddress(ni, xbpe, dispOrAddr) & 0xFF;
                Device d = getDevice(devNum);
                boolean ready = d != null && d.test();
                regs.SW = ready ? 0x40 : 0x00; // = if ready, < if not ready
                return false;
            }
            case Opcode.RD: { // read device -> A low byte
                int devNum = effectiveAddress(ni, xbpe, dispOrAddr) & 0xFF;
                Device d = getDevice(devNum);
                int val = (d != null) ? d.read() : 0;
                regs.A = (regs.A & 0xFFFF00) | (val & 0xFF);
                return false;
            }
            case Opcode.WD: { // write device from A low byte
                int devNum = effectiveAddress(ni, xbpe, dispOrAddr) & 0xFF;
                Device d = getDevice(devNum);
                if (d != null)
                    d.write(regs.A & 0xFF);
                return false;
            }
            default:
                notImplemented(Opcode.getName(opcode));
                return false;
        }
    }

    // Pomožna: razreši naslov glede na n/i in vrni naslov
    private int effectiveAddress(int ni, int xbpe, int dispOrAddr) {
        boolean n = (ni & 0x2) != 0;
        boolean i = (ni & 0x1) != 0;
        boolean x = ((xbpe >> 3) & 1) == 1;
        boolean b = ((xbpe >> 2) & 1) == 1;
        boolean p = ((xbpe >> 1) & 1) == 1;
        boolean e = (xbpe & 1) == 1;

        int addr;
        if (e) {
            addr = dispOrAddr & Memory.MAX_ADDRESS;
        } else {
            int disp = dispOrAddr & 0xFFF;
            if ((disp & 0x800) != 0)
                disp |= 0xFFFFF000; // sign-extend 12-bit
            if (p)
                addr = (regs.PC + disp) & Memory.MAX_ADDRESS;
            else if (b)
                addr = (regs.B + disp) & Memory.MAX_ADDRESS;
            else
                addr = disp & Memory.MAX_ADDRESS;
        }
        if (x)
            addr = (addr + regs.X) & Memory.MAX_ADDRESS;

        if (n && !i) {
            int target = mem.getWord(addr);
            return target & Memory.MAX_ADDRESS;
        }
        return addr & Memory.MAX_ADDRESS;
    }

    // Pomožna: razreši naslov in preberi besedo za LDA
    private int loadByAddressing(int ni, int xbpe, int dispOrAddr) {
        boolean n = (ni & 0x2) != 0;
        boolean i = (ni & 0x1) != 0;
        if (n && i) {
            return dispOrAddr & 0xFFFFFF; // immediate
        }
        int addr = effectiveAddress(ni, xbpe, dispOrAddr);
        return mem.getWord(addr);
    }

    // =====================
    // Simulacija procesorjeve ure z Timer
    // =====================

    // Zažene oz. nadaljuje samodejno izvajanje.
    void start() {
        if (running)
            return;
        running = true;
        timer = new java.util.Timer("SICXE-Timer", true);
        // Perioda v ms: 1 kHz = 1 ms med tiku; za speedKHz uporabimo 1000/speedKHz
        long periodMs = Math.max(1, 1000 / Math.max(1, speedKHz));
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                if (!running)
                    return;
                for (int i = 0; i < opsPerTick; i++) {
                    try {
                        execute();
                    } catch (Exception e) {
                        System.err.println("Execution error: " + e.getMessage());
                        // Ustavimo ob napaki
                        stop();
                        break;
                    }
                }
            }
        }, 0, periodMs);
    }

    // Zaustavi samodejno izvajanje.
    void stop() {
        running = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    // Pove, ali je samodejno izvajanje trenutno v teku.
    boolean isRunning() {
        return running;
    }

    // Branje hitrosti (kHz)
    int getSpeed() {
        return speedKHz;
    }

    // Nastavi hitrost (kHz). Večja hitrost -> krajša perioda časovnika.
    void setSpeed(int kHz) {
        if (kHz <= 0) {
            throw new IllegalArgumentException("Speed (kHz) must be positive");
        }
        speedKHz = kHz;
        // Če timer teče, ga ponovno zaženemo z novo periodo
        if (running) {
            stop();
            start();
        }
    }

    // =====================
    // Nalagalnik kontrolne sekcije (.obj z zapisi H/T/E)
    // =====================

    // Naloži absolutno sekcijo iz Readerja. Podpira standardne SIC/XE zapise:
    // Hname(6) start(6) length(6)\n
    // Taddr(6) len(2) data(2*len) [brez RESB/RESW; ničle po potrebi] \n
    // Eaddr(6)\n
    // Vrne true ob uspehu; PC se nastavi na entry naslov iz E zapisa.
    boolean loadSection(java.io.Reader r) {
        try {
            Utils.skipWhitespace(r);
            int rec = r.read();
            if (rec != 'H')
                throw new java.io.IOException("Expected H record");

            // Header: ime (6), start (6 hex), length(6 hex)
            String name = Utils.readString(r, 6);
            int start = Utils.readWordHex(r);
            int length = Utils.readWordHex(r);
            // Absolutni nalagalnik: lahko bi uporabili 'start' kot osnovo, a T zapisi
            // podajo polne naslove.
            // Ime in dolžina trenutno nista potrebna, vendar ju preberemo zaradi skladnosti
            // s formatom.

            // Preberemo dokler ne naletimo na E
            while (true) {
                Utils.skipWhitespace(r);
                r.mark(1);
                int rc = r.read();
                if (rc == -1)
                    throw new java.io.IOException("Unexpected EOF");
                if (rc == 'T') {
                    int addr = Utils.readAddress20(r);
                    int len = Utils.readByte(r); // dolžina v bajtih
                    for (int i = 0; i < len; i++) {
                        int b = Utils.readByte(r);
                        int target = (addr + i) & Memory.MAX_ADDRESS;
                        mem.setByte(target, b);
                    }
                } else if (rc == 'E') {
                    int entry = Utils.readAddress20(r);
                    regs.PC = entry & 0xFFFFF;
                    return true;
                } else {
                    throw new java.io.IOException("Unknown record type: " + (char) rc);
                }
            }
        } catch (java.io.IOException ex) {
            System.err.println("loadSection error: " + ex.getMessage());
            return false;
        }
    }
}
