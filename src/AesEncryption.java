import java.util.Random;

public class AesEncryption implements Encryption{

    private int[] sBox;
    private int[] invSBox;
    private final String encryptionKey;
    private static final int[][] CONSTANT_MATRIX = new int[][]{
            {2, 3, 1, 1},
            {1, 2, 3, 1},
            {1, 1, 2, 3},
            {3, 1, 1, 2}
    };
    private static final int[][] INV_CONSTANT_MATRIX = new int[][]{
            {0xE, 0xB, 0xD, 0x9},
            {0x9, 0xE, 0xB, 0xD},
            {0xD, 0x9, 0xE, 0xB},
            {0xB, 0xD, 0x9, 0xE}
    };
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private int[] rc;
    private final int numRounds;
    private final int numWords;
    private final int n;


    /* First Index is the wordNumber 0 -> 44, 52, 60
     * Second Index is the byteNumber  0 -> 3
     */
    private int[][] roundKeys;

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

    public void calculateRijndaelSBox() {
        int[] sBox = new int[256];
        int[] invSBox = new int[256];

        for (int i = 0; i < 256; i++) {
            int s = getGFInverse(i);
            int x = s;

            for (int count = 0; count < 4; count++) {
                s = s << 1;
                if (s >= 0x100)
                    s = s - 0x100 + 1;
                x = s ^ x;

            }
            int value = x ^ 99;
            sBox[i] = value;
            invSBox[value] = i;
        }
        this.invSBox = invSBox;
        this.sBox = sBox;
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

    // Operation XOR on two words and returns the result
    private int[] xorWords(int[] word1, int[] word2) {
        int[] result = new int[4];
        for (int i = 0; i < 4; i++) {
            result[i] = word1[i] ^ word2[i];
        }
        return result;
    }

    private int[] generateNextWord(int wordNumber) {
        int[] g;
        if (wordNumber % n == 0) {
            g = new int[4];
            System.arraycopy(roundKeys[wordNumber - 1], 0, g, 0, 4);
            circularLeftShift1(g);
            substituteRow(g, sBox);
            g[0] ^= rc[wordNumber/n];
        } else if (n == 8 && wordNumber % 8 == 4) {
            g = new int[4];
            System.arraycopy(roundKeys[wordNumber - 1], 0, g, 0, 4);
            substituteRow(g, sBox);
        } else {
            g = roundKeys[wordNumber - 1];
        }

        return xorWords(roundKeys[wordNumber - n], g);
    }

    private void generateRoundKey() {

        roundKeys = new int[numWords][4];
        int numKeyBytes = n * 4;

        // First round
        if (encryptionKey.length() == numKeyBytes) {
            for (int index = 0; index < numKeyBytes; index++)
                roundKeys[index / 4][index % 4] = encryptionKey.charAt(index);
        } else if (encryptionKey.length() == numKeyBytes * 2) {
            for (int index = 0; index < numKeyBytes; index++)
                roundKeys[index / 4][index % 4] = (Character.digit(encryptionKey.charAt(index * 2), 16) << 4) +
                        Character.digit(encryptionKey.charAt(index * 2 + 1), 16);
        } else {
            throw new RuntimeException("Invalid Key Length");
        }

        // Generate RC values
        rc = new int[numRounds + 1];
        rc[1] = 1;
        for(int i = 2; i <= numRounds; i++)
            rc[i] = multiplyGF(2, rc[ i - 1]);

        // Generate words for the rest of the rounds
        for(int index = n; index < numWords; index++) {
            roundKeys[index] = generateNextWord(index);
        }
    }

    /* Converts plainText into its byte equivalent so that we can work with it.
     * The first index points to the 4x4 state matrix of portion of plainText we are looking at.
     * The second and third index denote to the byte in 4x4 state matrix
     */
    private int[][][] convertPlainTextToByteMatrices(String plainText) {

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

    private String convertByteMatricesToPlainText(int[][][] byteMatrices) {

        StringBuilder plainText = new StringBuilder();
        for (int[][] byteMatrix : byteMatrices) {
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 4; row++) {
                    if (byteMatrix[row][col] == 0) {
                        return plainText.toString();
                    } else {
                        plainText.append((char) byteMatrix[row][col]);
                    }
                }
            }
        }
        return plainText.toString();
    }

    // Substitute Bytes with sBox in-place for a row
    private void substituteRow(int[] row, int[] sBox) {
        for (int col = 0; col < 4; col++) {
            row[col] = sBox[row[col]];
        }
    }
    // Substitute Bytes with the sBox in-place for a state
    private void substituteBytes(int[][] state, int[] sBox) {
        for (int row = 0; row < 4; row++) {
            substituteRow(state[row], sBox);
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

    private void invShiftRow(int[][] state) {
        circularLeftShift3(state[1]);
        circularLeftShift2(state[2]);
        circularLeftShift1(state[3]);
    }

    // Mix Columns
    private void mixColumns(int[][] state, final int[][] matrix) {
        int[][] newState = new int[4][4];
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++) {

                /* Calculate the value at [row][col].
                 * Row refers to which row in constantMatrix
                 * Col refers to which col in the state
                 * */
                for (int otherIndex = 0; otherIndex < 4; otherIndex++) {
                    newState[row][col] ^= multiplyGF(matrix[row][otherIndex], state[otherIndex][col]);
                }
            }
        // Copy the row over to the old matrix
        System.arraycopy(newState, 0, state, 0, 4);
    }

    private void addRoundKey(int[][] state, int roundNumber) {
        int wordIncrement = roundNumber * 4;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                state[row][col] ^= roundKeys[col + wordIncrement][row];
            }
        }
    }

    private int[][][] convertHexTextToMatrix(String hexText){
        //Every 2 characters is one byte
        int numBytes = hexText.length()/2;
        int numStates = numBytes/16 + ((numBytes % 16 == 0)? 0: 1);
        int[][][] byteMatrices = new int[numStates][4][4];

        int hexTextIndex = 0;
        for (int stateIndex = 0; stateIndex < numStates; stateIndex++) {
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 4; row++) {
                    if (hexTextIndex == hexText.length()) {
                        return byteMatrices;
                    } else {
                        byteMatrices[stateIndex][row][col] = (Character.digit(hexText.charAt(hexTextIndex),16) << 4) +
                                Character.digit(hexText.charAt(++hexTextIndex), 16);
                        hexTextIndex++;
                    }
                }
            }
        }
        return byteMatrices;
    }

    private String convertMatrixToHexText(int[][][] byteMatrices) {
        StringBuilder text = new StringBuilder();
        for (int[][] byteMatrix : byteMatrices)
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 4; row++) {
                    if (byteMatrix[row][col] < 0x10) text.append('0');
                    text.append(Integer.toString(byteMatrix[row][col], 16));
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

        this.encryptionKey = encryptionKey;
        calculateRijndaelSBox();

        if (bits == 128) {
            n = 4; // Key but be length 16 or 32
        } else if (bits == 192) {
            n = 6; // Key length 24 or 48
        } else if (bits == 256) {
            n = 8; // Key length 32 or 64
        } else
            throw new RuntimeException("Invalid Bit Size");

        numRounds = n + 6;
        numWords = (numRounds + 1) * 4;
        generateRoundKey();
    }

    public String getKey() {
        return encryptionKey;
    }

    @Override
    public String encrypt(String plainText) {

        int[][][] byteMatrices = convertPlainTextToByteMatrices(plainText);

        for (int[][] byteMatrix : byteMatrices) {
            addRoundKey(byteMatrix, 0);

            for (int roundNumber = 1; roundNumber < rc.length - 1; roundNumber++) {
                substituteBytes(byteMatrix, sBox);
                shiftRow(byteMatrix);
                mixColumns(byteMatrix, CONSTANT_MATRIX);
                addRoundKey(byteMatrix, roundNumber);
            }

            substituteBytes(byteMatrix, sBox);
            shiftRow(byteMatrix);
            addRoundKey(byteMatrix, rc.length - 1);
        }

        return convertMatrixToHexText(byteMatrices);
    }

    @Override
    public String decrypt(String message) {
        int[][][] byteMatrices = convertHexTextToMatrix(message);

        for (int[][] byteMatrix: byteMatrices) {
            addRoundKey(byteMatrix, rc.length - 1);
            invShiftRow(byteMatrix);
            substituteBytes(byteMatrix, invSBox);

            for (int roundNumber = rc.length - 2; roundNumber > 0; roundNumber--) {
                addRoundKey(byteMatrix, roundNumber);
                mixColumns(byteMatrix, INV_CONSTANT_MATRIX);
                invShiftRow(byteMatrix);
                substituteBytes(byteMatrix, invSBox);
            }
            addRoundKey(byteMatrix, 0);
        }
        return convertByteMatricesToPlainText(byteMatrices);
    }

    // The following functions are used mainly for debugging purposes
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

    private void printRoundKeys() {
        System.out.println("Round Key: ");
        for (int i = 0; i < roundKeys.length; i++) {
            for (int j = 0; j < roundKeys[0].length; j++) {
                System.out.print(Integer.toString(roundKeys[i][j], 16) + " ");
            }
            System.out.println();
        }
    }
}


