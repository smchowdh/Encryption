
public class Main {
    public static void main(String[] args) {

        System.out.println("Hello and welcome!");

        Encryption aesEncryption = new AesEncryption("Thats my Kung Fu");
        String plainText = "Two One Nine Two";
        String encryptedText = aesEncryption.encrypt(plainText);
        System.out.println(encryptedText);
    }
}