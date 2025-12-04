import java.io.BufferedReader;
import java.io.FileReader;

public class Runner {
    public static void main(String[] args) throws Exception {
        // Nastavi poti po potrebi
        String bootObj = "c:\\Users\\Administrator\\Documents\\SPO\\VAJE\\boot.obj"; // zamenjaj, če imaš drugo pot
        String f1dev = "c:\\Users\\Administrator\\Documents\\SPO\\VAJE\\F1.dev"; // datoteka z heks podatki

        Machine m = new Machine(1 << 20); // 1 MB
        // Pripni datotečno napravo na ID 0xF1
        m.setDevice(0xF1, new FileDevice(0xF1, f1dev));

        // Naloži bootstrap nalagalnik iz .obj (H/T/E)
        try (BufferedReader r = new BufferedReader(new FileReader(bootObj))) {
            boolean ok = m.loadSection(r);
            if (!ok) {
                System.err.println("Failed to load boot.obj");
                return;
            }
        }

        // Po želji: preveri začetni PC
        System.out.printf("PC after load: 0x%05X\n", m.regs.PC);

        // Zaženi izvajanje
        m.setSpeed(5); // 5 kHz, lahko spremeniš
        m.start();

        // Pusti zagnano nekaj časa (npr. 3 sekunde), nato ustavi
        Thread.sleep(3000);
        m.stop();
        System.out.println("Stopped.");
    }
}
