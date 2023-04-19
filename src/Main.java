// Press ⇧ twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) {
        // Press ⌥⏎ with your caret at the highlighted text to see how
        // IntelliJ IDEA suggests fixing it.
        System.out.println("Hello and welcome!");

        Encryption aesEncryption = new AesEncryption("Thats my Kung Fu");
        String plainText = "Two One Nine Two";
        String encryptedText = aesEncryption.encrypt(plainText);
        System.out.println(encryptedText);
    }
}