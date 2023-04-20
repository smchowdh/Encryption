import java.util.Random;

public class AesEncryption implements Encryption{

    private final int[] sbox;
    private final String encryptionKey;
    private int[] encryptionKeyInBytes;
    private static final int[][] CONSTANT_MATRIX = new int[][]{
            {2, 3, 1, 1},
            {1, 2, 3, 1},
            {1, 1, 2, 3},
            {3, 1, 1, 2}
    };
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private int[] rc;

    /* First Index is the roundNumber 0 -> 10, 12, 14
     * Second Index is the wordNumber 0 -> 3
     * Third Index is the byteNumber  0 -> 3
     */
    private int[][][] roundKeys;
    public static void main (String[] args) {
        AesEncryption x = new AesEncryption("Thats my Kung Fu", 128);
        System.out.println(x.encrypt("Two One Nine Two"));

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
                if (s >= 0x100)
                    s = s - 0x100 + 1;
                x = s ^ x;

            }
            sbox[i] = x ^ 99;
        }
        return sbox;
    }

    private static String generateRandomKey() {
        StringBuilder key = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 16; i++) {
            int index = random.nextInt(ALPHABET.length());
            key.append(ALPHABET.charAt(index));
        }
        return key.toString();
    }

    // Operation XOR on two words and returning the result
    private int[] xorWords(int[] word1, int[] word2) {
        int[] result = new int[4];
        for (int i = 0; i < 4; i++) {
            result[i] = word1[i] ^ word2[i];
        }
        return result;
    }

    private int[][] generateNextRound(int[][] round, int roundNumber) {
        int[][] nextRound = new int[4][4];
        int[] g = new int[4];
        System.arraycopy(round[3], 0, g, 0, 4);
        circularLeftShift1(g);
        substituteRow(g);
        g[0] ^= rc[roundNumber];
        nextRound[0] = xorWords(round[0], g);

        for (int i = 1; i < 4; i++) {
            nextRound[i] = xorWords(round[i], nextRound[i - 1]);
        }

        return nextRound;
    }

    private void generateRoundKey(int numRounds) {
        // Generate RC values
        rc = new int[numRounds + 1];
        rc[1] = 1;
        for(int i = 2; i <= numRounds; i++) {
            rc[i] = multiplyGF(2, rc[i-1]);

        }

        roundKeys = new int[numRounds + 1][4][4];
        encryptionKeyInBytes = new int[16];

        // First round
        for (int index = 0; index < 16; index++) {
            int value = encryptionKey.charAt(index);
            roundKeys[0][index/4][index % 4] = value;
            encryptionKeyInBytes[index] = value;
        }

        // Rest of the rounds
        for(int index = 1; index <= numRounds; index++) {
            roundKeys[index] = generateNextRound(roundKeys[index - 1], index);
        }

        // For some reason, states are represent in the Matrix transform, where each word is a column, so we are going to get the transpose each round key

        for(int roundIndex = 0; roundIndex <= numRounds; roundIndex++) {
            for (int row = 0; row < 4; row++) {
                for (int col = row + 1; col < 4; col++){
                    int temp = roundKeys[roundIndex][row][col];
                    roundKeys[roundIndex][row][col] = roundKeys[roundIndex][col][row];
                    roundKeys[roundIndex][col][row] = temp;
                }
            }
        }
    }

    /* Converts plainText into its byte equivalent so that we can work with it.
     * The first index points to the 4x4 state matrix of portion of plainText we are looking at.
     * The second and third index denote to the byte in 4x4 state matrix

     */
    private int[][][] convertToByteMatrices(String plainText) {

        //Every character is one byte
        int numBytes = plainText.length();
        int numStates = numBytes/16 + ((numBytes % 16 == 0)? 0: 1);
        int[][][] byteMatrices = new int[numStates][4][4];

        int plainTextIndex = 0;
        for (int stateIndex = 0; stateIndex < numStates; stateIndex++) {
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 4; row++) {
                    if (plainTextIndex == numBytes) {
                        return byteMatrices;
                    } else {
                        byteMatrices[stateIndex][row][col] = plainText.charAt(plainTextIndex);
                        plainTextIndex++;
                    }
                }
            }
        }
        return byteMatrices;
    }

    // Substitute Bytes with sbox in-place for a row
    private void substituteRow(int[] row) {
        for (int col =0; col < 4; col++) {
            row[col] = sbox[row[col]];
        }
    }
    // Substitute Bytes with the sbox in-place for a state
    private void substituteBytes(int[][] state) {
        for (int row = 0; row < 4; row++) {
            substituteRow(state[row]);
        }
    }

    private void circularLeftShift1 (int[] row) {
        int temp = row[0];
        for (int col = 1; col < 4; col++) {
            row[col - 1] = row[col];
        }
        row[3] = temp;
    }

    private void circularLeftShift2 (int[] row) {
        for (int col = 0; col < 2; col++) {
            int temp = row[col];
            row[col] = row[col + 2];
            row[col + 2] = temp;
        }
    }

    private void circularLeftShift3 (int[] row) {
        // Row 3
        int temp = row[3];
        for (int col = 3; col > 0; col--){
            row[col] = row[col - 1];
        }
        row[0] = temp;
    }
    // shiftRows in place
    private void shiftRow(int[][] state) {
        circularLeftShift1(state[1]);
        circularLeftShift2(state[2]);
        circularLeftShift3(state[3]);
    }

    // Mix Columns
    private void mixColumns(int[][] state) {
        int[][] newState = new int[4][4];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {

                /* Calculate the value at [row][col].
                 * Row refers to which row in constantMatrix
                 * Col refers to which col in the state
                 * */
                for (int otherIndex = 0; otherIndex < 4; otherIndex++) {
                    newState[row][col] ^= multiplyGF(CONSTANT_MATRIX[row][otherIndex], state[otherIndex][col]);
                }
            }
        }
        // Copy the row over to the old matrix
        for (int row = 0; row < 4; row++)
            state[row] = newState[row];
    }

    private void addRoundKey(int[][] state, int roundNumber) {
        int[][] round = roundKeys[roundNumber];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                state[row][col] ^= round[row][col];
            }
        }
    }

    private String convertToHexText(int[][][] byteMatrices) {
        StringBuilder text = new StringBuilder();
        for (int stateIndex = 0; stateIndex < byteMatrices.length; stateIndex++)
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 4; row++) {
                    text.append(Integer.toString(byteMatrices[stateIndex][row][col],16));
                }
            }

        return text.toString();
    }

    // Default constructors implies random key string and AES-256
    public AesEncryption() {
        this(generateRandomKey(), 256);
    }

    public AesEncryption(String encryptionKey) {
        this(encryptionKey, 256);
    }

    public AesEncryption(int bits) {
        this(generateRandomKey(), bits);
    }

    public AesEncryption(String encryptionKey, int bits) {
        System.out.println(encryptionKey);
        if (encryptionKey.length() != 16 || !(bits == 128 || bits == 192 || bits == 256)) {
            throw new RuntimeException();
        }
        this.encryptionKey = encryptionKey;

        sbox = calculateRijndaelSBox();
        if (bits == 128)
            generateRoundKey(10);
        else if (bits == 192)
            generateRoundKey(12);
        else
            generateRoundKey(14);
    }

    public String getStringKey() {
        return encryptionKey;
    }

    public String getHexKey() {
        StringBuilder hexKey = new StringBuilder();
        for (int i = 0; i < 16; i++){
            hexKey.append(Integer.toString(encryptionKeyInBytes[i],16));
        }
        return hexKey.toString();
    }

    @Override
    public String encrypt(String plainText) {

        int[][][] byteMatrices = convertToByteMatrices(plainText);

        for (int[][] byteMatrix : byteMatrices) {
            addRoundKey(byteMatrix, 0);

            for (int roundNumber = 1; roundNumber < rc.length - 1; roundNumber++) {
                // Substitute Bytes
                substituteBytes(byteMatrix);
                shiftRow(byteMatrix);
                mixColumns(byteMatrix);
                addRoundKey(byteMatrix, roundNumber);
            }

            substituteBytes(byteMatrix);
            shiftRow(byteMatrix);
            addRoundKey(byteMatrix, rc.length - 1);
        }

        return convertToHexText(byteMatrices);
    }

    @Override
    public String decrypt(String message) {
        return null;
    }

    // The following is used mainly for debugging purposes
    private void printWord(int[] word) {
        for (int col = 0; col < 4; col++) {
            System.out.print(" " + Integer.toString(word[col], 16));
        }
    }
    private void printState(int[][] state) {
        for (int row = 0; row < 4; row++) {
            printWord(state[row]);
            System.out.println();
        }
    }
    private void printByteMatrices(int[][][] byteMatrices) {
        for (int stateIndex = 0; stateIndex < byteMatrices.length; stateIndex++) {
            System.out.println("\nState " + stateIndex + ":");
            printState(byteMatrices[stateIndex]);
        }
    }
}


