package encryption;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

public class AesEncryption implements Encryption {

    private static int[] rc;
    private static int[] sBox;
    private static int[] invSBox;
    private static final int[][] CONSTANT_MATRIX = new int[][]{
            { 2, 3, 1, 1 },
            { 1, 2, 3, 1 },
            { 1, 1, 2, 3 },
            { 3, 1, 1, 2 }
    };
    private static final int[][] INV_CONSTANT_MATRIX = new int[][]{
            { 0xE, 0xB, 0xD, 0x9 },
            { 0x9, 0xE, 0xB, 0xD },
            { 0xD, 0x9, 0xE, 0xB },
            { 0xB, 0xD, 0x9, 0xE }
    };
    private static final String ALPHABET = "abcdef0123456789";
    private final String encryptionKey;
    private final int numRounds;
    private final int numWords;
    private final int n;

    /* First Index is the wordNumber 0 -> 44, 52, 60
     * Second Index is the byteNumber  0 -> 3
     */
    private int[][] roundKeys;

    static {
        calculateRijndaelSBox();
        calculateRC();
    }

    private static int multiplyGF(int num1, int num2) {

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

    //Brute force obtains the inverse
    private static int getGFInverse(int x) {

        if (x == 0)
            return 0;

        for (int i = 1; i < 255; i++)
            if (multiplyGF(x, i) == 1)
                return i;

        // It is guaranteed that an inverse exists so if 0 - 254 is checked, then it must be 255
        return 255;
    }

    private static void calculateRijndaelSBox() {

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
        AesEncryption.invSBox = invSBox;
        AesEncryption.sBox = sBox;
    }
    private static void calculateRC() {

        rc = new int[15];
        rc[1] = 1;

        for(int i = 2; i < 15; i++)
            rc[i] = multiplyGF(2, rc[ i - 1]);
    }

    private String generateRandomKey() {

        StringBuilder key = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < n * 8; i++) {
            int index = random.nextInt(ALPHABET.length());
            key.append(ALPHABET.charAt(index));
        }

        return key.toString();
    }

    // XOR on two words and returns the result in a new Array
    private static int[] xorWords(int[] word1, int[] word2) {

        int[] result = new int[4];

        for (int i = 0; i < 4; i++) {
            result[i] = word1[i] ^ word2[i];
        }
        return result;
    }

    // XOR on two matrices stores in matrix1
    private void xorMatrix(int[][] matrix1, int[][] matrix2) {

        for (int index = 0; index < 4; index++) {
            matrix1[index] = xorWords(matrix1[index], matrix2[index]);
        }
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
            for (int index = 0; index < numKeyBytes; index++) {
                roundKeys[index / 4][index % 4] = encryptionKey.charAt(index);
            }
        } else if (encryptionKey.length() == numKeyBytes * 2 && Pattern.matches("^[0-9a-f]+$", encryptionKey)) {

            for (int index = 0; index < numKeyBytes; index++) {
                roundKeys[index / 4][index % 4] = (Character.digit(encryptionKey.charAt(index * 2), 16) << 4) +
                        Character.digit(encryptionKey.charAt(index * 2 + 1), 16);
            }
        } else {
            throw new IllegalArgumentException("Invalid Key");
        }

        // Generate words for the rest of the rounds
        for(int index = n; index < numWords; index++) {
            roundKeys[index] = generateNextWord(index);
        }
    }

    private int[][] convertPlainTextToByteMatrix(String plainText, int plainTextIndex) {

        int[][] matrix = new int[4][4];

        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                matrix[row][col] = plainText.charAt(plainTextIndex++);
            }
        }

        return matrix;
    }

    /* Converts plainText into its byte equivalent so that we can work with it.
     * The first index points to the 4x4 state matrix of portion of plainText we are looking at.
     * The second and third index denote to the byte in 4x4 state matrix
     */
    private int[][][] convertPlainTextToByteMatrices(String plainText) {

        // Every character is one byte
        int numBytes = plainText.length();
        int numStates = numBytes/16 + 1;
        int[][][] byteMatrices = new int[numStates][][];
        int lastStateIndex = numStates - 1;
        int plainTextIndex = 0;

        for (int stateIndex = 0; stateIndex < lastStateIndex; stateIndex++) {
            byteMatrices[stateIndex] = convertPlainTextToByteMatrix(plainText, plainTextIndex);
            plainTextIndex += 16;
        }

        // Last Matrix plus padding
        int[][] lastState = new int[4][4];
        int padding = numStates * 16 - numBytes;
        int startPaddingIndex = 16 - padding;

        for (int index = 0; index < startPaddingIndex; index++) {
            lastState[index % 4][index / 4] = plainText.charAt(plainTextIndex++);
        }

        while (startPaddingIndex < 16) {
            lastState[startPaddingIndex % 4][startPaddingIndex++ / 4] = padding;
        }

        byteMatrices[lastStateIndex] = lastState;
        return byteMatrices;
    }

    private int[][] convertHexTextToMatrix(String hexText, int hexTextIndex) {

        int[][] matrix = new int[4][4];

        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                matrix[row][col] = (Character.digit(hexText.charAt(hexTextIndex++),16) << 4) +
                        Character.digit(hexText.charAt(hexTextIndex++), 16);
            }
        }

        return matrix;
    }

    private int[][][] convertHexTextToMatrices(String hexText) {

        if (hexText.length() % 32 != 0 || !Pattern.matches("^[0-9a-f]+$", hexText)) {
            throw new IllegalArgumentException("Invalid Decrypt Text");
        }

        // Every 2 characters is one byte
        int numBytes = hexText.length()/2;
        int numStates = numBytes/16;
        int[][][] byteMatrices = new int[numStates][4][4];

        for (int stateIndex = 0; stateIndex < numStates; stateIndex++) {
            byteMatrices[stateIndex] = convertHexTextToMatrix(hexText, stateIndex * 32);
        }

        return byteMatrices;
    }

    private String convertByteMatricesToPlainText(int[][][] byteMatrices) {

        StringBuilder plainText = new StringBuilder();
        int lastMatrixIndex = byteMatrices.length - 1;

        for (int stateIndex = 0; stateIndex < lastMatrixIndex; stateIndex++) {
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 4; row++) {
                    plainText.append((char) byteMatrices[stateIndex][row][col]);
                }
            }
        }

        int[][] lastMatrix = byteMatrices[lastMatrixIndex];
        int padding = lastMatrix[3][3];

        if (padding > 0x10) {
            throw new IllegalArgumentException("Invalid Decrypt Padding");
        }
        int startPaddingIndex = 16 - padding;

        for (int i = startPaddingIndex; i < 16; i++) {
            if (lastMatrix[i % 4][i / 4] != padding) {
                throw new IllegalArgumentException("Invalid Decrypt Padding");
            }
        }

        for (int i = 0; i < startPaddingIndex; i++) {
            plainText.append((char) lastMatrix[i % 4][i / 4]);
        }

        return plainText.toString();
    }

    private int[][] getInitialState(String initializationVector) {

        if (initializationVector.length() == 32 && Pattern.matches("^[0-9a-f]+$",initializationVector)) {
            return convertHexTextToMatrix(initializationVector, 0);
        } else if (initializationVector.length() != 16) {
            throw new IllegalArgumentException("Invalid initializationVector");
        } else {
            return convertPlainTextToByteMatrix(initializationVector, 0);
        }
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

    private void encryptBlock(int[][] byteMatrix) {

        addRoundKey(byteMatrix, 0);

        for (int roundNumber = 1; roundNumber < numRounds; roundNumber++) {
            substituteBytes(byteMatrix, sBox);
            shiftRow(byteMatrix);
            mixColumns(byteMatrix, CONSTANT_MATRIX);
            addRoundKey(byteMatrix, roundNumber);
        }

        substituteBytes(byteMatrix, sBox);
        shiftRow(byteMatrix);
        addRoundKey(byteMatrix, numRounds);
    }

    private void decryptBlock(int[][] byteMatrix) {

        addRoundKey(byteMatrix, numRounds);
        invShiftRow(byteMatrix);
        substituteBytes(byteMatrix, invSBox);

        for (int roundNumber = numRounds - 1; roundNumber > 0; roundNumber--) {
            addRoundKey(byteMatrix, roundNumber);
            mixColumns(byteMatrix, INV_CONSTANT_MATRIX);
            invShiftRow(byteMatrix);
            substituteBytes(byteMatrix, invSBox);
        }

        addRoundKey(byteMatrix, 0);
    }

    // Default constructors implies random key string and AES-128
    public AesEncryption() {
        this(null, 128);
    }

    public AesEncryption(String encryptionKey) {
        this(encryptionKey, 128);
    }

    public AesEncryption(int bits) {
        this(null, bits);
    }

    public AesEncryption(String encryptionKey, int bits) {

        n = switch (bits) {
            case 128 -> 4;
            case 192 -> 6;
            case 256 -> 8;
            default -> throw new IllegalArgumentException("Invalid Bit Size");
        };

        this.encryptionKey = Objects.requireNonNullElseGet(encryptionKey, this::generateRandomKey);
        numRounds = n + 6;
        numWords = (numRounds + 1) * 4;
        generateRoundKey();
    }

    public String getKey() {
        return encryptionKey;
    }

    // Encrypt in ECB
    @Override
    public String encrypt(String plainText) {

        int[][][] byteMatrices = convertPlainTextToByteMatrices(plainText);

        for (int[][] byteMatrix : byteMatrices) {
            encryptBlock(byteMatrix);
        }

        return convertMatrixToHexText(byteMatrices);
    }

    // Encrypt in CBC
    public String encrypt(String plainText, String initializationVector) {

        int[][][] byteMatrices = convertPlainTextToByteMatrices(plainText);
        int[][] state = getInitialState(initializationVector);

        for (int[][] byteMatrix : byteMatrices) {
            xorMatrix(byteMatrix, state);
            encryptBlock(byteMatrix);
            state = byteMatrix;
        }

        return convertMatrixToHexText(byteMatrices);
    }

    // Decrypt in EBC
    @Override
    public String decrypt(String cypherText) {

        int[][][] byteMatrices = convertHexTextToMatrices(cypherText);
        Arrays.stream(byteMatrices).forEach(this::decryptBlock);
        return convertByteMatricesToPlainText(byteMatrices);
    }

    // Decrypt in CBC
    public String decrypt(String cypherText, String initializationVector) {

        int[][][] byteMatrices = convertHexTextToMatrices(cypherText);

        for (int stateIndex = byteMatrices.length - 1; stateIndex > 0;) {
            int[][] byteMatrix = byteMatrices[stateIndex];
            decryptBlock(byteMatrix);
            xorMatrix(byteMatrix, byteMatrices[--stateIndex]);
        }

        int[][] firstMatrix = byteMatrices[0];
        decryptBlock(firstMatrix);
        xorMatrix(firstMatrix, getInitialState(initializationVector));
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

        for (int[] roundKey : roundKeys) {
            for (int j = 0; j < roundKeys[0].length; j++) {
                System.out.print(Integer.toString(roundKey[j], 16) + " ");
            }
            System.out.println();
        }
    }
}


