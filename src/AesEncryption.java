import javax.crypto.KeyGenerator;
import java.util.Arrays;

public class AesEncryption implements Encryption{

    private int[] sbox;
    private String encryptionKey;

    public static void main (String[] args) {
        AesEncryption x = new AesEncryption("lemon");
        x.encrypt("Two One Nine Two");

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

    public AesEncryption() {
        sbox = calculateRijndaelSBox();
    }
    public AesEncryption(String encryptionKey) {
        this.encryptionKey = encryptionKey;
        sbox = calculateRijndaelSBox();
    }

    /* Converts plainText into its byte equivalent so that we can work with it.
     * The first index points to the 4x4 state matrix of portion of plainText we are looking at.
     * The second and third index denote to the byte in 4x4 state matrix

     */
    private int[][][] convertToBytes(String plainText) {

        //Every character is one byte
        int numBytes = plainText.length();
        int numStates = numBytes/16 + ((numBytes % 16 == 0)? 0: 1);
        int[][][] byteMatrix = new int[numStates][4][4];

        int plainTextIndex = 0;
        for (int stateIndex = 0; stateIndex < numStates; stateIndex++) {
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    if (plainTextIndex == numBytes) {
                        return byteMatrix;
                    } else {
                        byteMatrix[stateIndex][row][col] = plainText.charAt(plainTextIndex);
                        plainTextIndex++;
                    }
                }
            }
        }
        return byteMatrix;
    }

    private void printByteMatrix(int[][][] byteMatrix) {
        for (int stateIndex = 0; stateIndex < byteMatrix.length; stateIndex++) {
            System.out.println("\nState " + stateIndex + ":");
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    System.out.print(" " + Integer.toString(byteMatrix[stateIndex][row][col], 16));
                }
                System.out.println();
            }
        }
    }
    @Override
    public String encrypt(String plainText) {

        int[][][] byteMatrix = convertToBytes(plainText);
        printByteMatrix(byteMatrix);
        // Substitute Bytes
        // Shift Rows
        // Mix Columns
        // Add round key
        return "lemon";
    }

    @Override
    public String decrypt(String message) {
        return null;
    }
}
