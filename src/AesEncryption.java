import java.util.Arrays;

public class AesEncryption implements Encryption{

    private int[] sbox;
    private String encryptionKey;

    public static void main (String[] args) {
        AesEncryption x = new AesEncryption("lemon");
        x.calculateRijndaelSBox();

    }

    private int multiplyGF(int num1, int num2) {

        //Multiply
        int result = 0;

        while (num2 != 0) {

            if (num2 % 2 == 1)
                result ^= num1;

            num1 = num1 << 1;
            num2 = num2 >> 1;
        }

        // Then mod 0x11B
        int divisor = 0x11b;
        int checker = 0x100;

        if (result >= checker) {
            do {
                divisor = divisor << 1;
                checker = checker << 1;
            }  while (result >= checker);
            divisor = divisor >> 1;
            checker = checker >> 1;
        }

        while (result >= 0x100) {
            if (result >= checker)
                result ^= divisor;
            divisor = divisor >> 1;
            checker = checker >> 1;
        }
        return result;
    }
    private int getGFInverse(int x) {
        if (x == 0)
            return 0;

        for (int i = 1; i < 256; i++)
            if (multiplyGF(x, i) == 1)
                return i;

        return -1;
    }

    public int[] calculateRijndaelSBox() {
        int[] sbox = new int[256];

        for (int i = 0; i < 256; i++) {
            int s = getGFInverse(i);
            int x = s;

            for (int count = 0; count < 4; count++) {
                s = s << 1;
                if (s > 0x100)
                    s = s - 0x100 + 1;
                x = s ^ x;

            }
            sbox[i] = x ^ 99;
        }

        return sbox;
    }

    public AesEncryption(String encryptionKey) {
        this.encryptionKey = encryptionKey;
        sbox = calculateRijndaelSBox();
    }

    @Override
    public String encrypt(String message) {

        return "lemon";
    }

    @Override
    public String decrypt(String message) {
        return null;
    }
}
