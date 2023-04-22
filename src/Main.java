import encryption.AesEncryption;
import encryption.Encryption;

public class Main {
    public static void main(String[] args) {

        // AES encryption
        // Using an example found from https://www.youtube.com/watch?v=Z_7aOkS8tOA

        Encryption aesEncryptionExample1 = new AesEncryption("password12345678password", 192);
        String plainText = "password12345678";
        String encryptedText = aesEncryptionExample1.encrypt(plainText);
        System.out.println("Encrypted Text: " + encryptedText);

        Encryption aesEncryptionExample2 = new AesEncryption("Thats my Kung Fu", 128);
        encryptedText = aesEncryptionExample2.encrypt("Two One Nine Twoo");
        System.out.println("Encrypted Text: " + encryptedText);
        String decryptedText = aesEncryptionExample2.decrypt("29c3505f571420f6402299b31a02d73ab3e46f11ba8d2b97c18769449a89e868");
        System.out.println("DecryptedText: " +  decryptedText);
    }
}