
public class Main {
    public static void main(String[] args) {

        // AES encryption
        // Using an example found from https://www.youtube.com/watch?v=Z_7aOkS8tOA

        Encryption aesEncryptionExample1 = new AesEncryption("password12345678password", 192);
        String plainText = "password12345678";
        String encryptedText = aesEncryptionExample1.encrypt(plainText);
        System.out.println("Encrypted Text: " + encryptedText);

        Encryption aesEncryptionExample2 = new AesEncryption("Thats my Kung Fu", 128);
        //String decryptedText = aesEncryptionExample2.decrypt(encryptedText);
        //System.out.println("DecryptedText: " +  decryptedText);
    }
}