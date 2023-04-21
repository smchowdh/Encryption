package test;

import encryption.AesEncryption;
import org.junit.Test;
import static org.junit.Assert.*;
import java.security.InvalidParameterException;
import java.util.regex.Pattern;

// Specific test data was found from: https://www.devglan.com/online-tools/aes-encryption-decryption
public class AesEncryptionTest {

    @Test
    public void TestDefaultConstructor() {
        AesEncryption aes = new AesEncryption();
        String key = aes.getKey();
        assertTrue(Pattern.matches("^[0-9a-f]+$", aes.getKey()));
        assertEquals(32, key.length());
    }

    @Test
    public void TestValidBitsConstructor() {
        AesEncryption aes128 = new AesEncryption(128);
        AesEncryption aes192 = new AesEncryption(192);
        AesEncryption aes256 = new AesEncryption(256);
        String key128 = aes128.getKey();
        assertTrue(Pattern.matches("^[0-9a-f]+$", aes128.getKey()));
        assertEquals(32, key128.length());
        String key192 = aes192.getKey();
        assertTrue(Pattern.matches("^[0-9a-f]+$", aes192.getKey()));
        assertEquals(48, key192.length());
        String key256 = aes256.getKey();
        assertTrue(Pattern.matches("^[0-9a-f]+$", aes256.getKey()));
        assertEquals(64, key256.length());
    }

    @Test
    public void TestInvalidBitsConstructor() {
        try {
            new AesEncryption(0);
        } catch (InvalidParameterException e) {
            assertEquals("Invalid Bit Size", e.getMessage());
        }
    }

    @Test
    public void TestInvalidKeyLengthConstructor() {
        try {
            new AesEncryption("1234");
        } catch (InvalidParameterException e) {
            assertEquals("Invalid Key Length", e.getMessage());
        }
    }

    @Test
    public void TestEncrypt() {
        AesEncryption aes = new AesEncryption("Thats my Kung Fu");
        String result = aes.encrypt("Two One Nine Two");
        assertEquals("29c3505f571420f6402299b31a02d73ab3e46f11ba8d2b97c18769449a89e868", result);
    }

    @Test
    public void TestDecrypt() {
        AesEncryption aes = new AesEncryption("Thats my Kung Fu");
        String result = aes.decrypt("29c3505f571420f6402299b31a02d73ab3e46f11ba8d2b97c18769449a89e868");
        assertEquals("Two One Nine Two", result);
    }

    @Test
    public void TestEncryptPadding() {
        AesEncryption aes = new AesEncryption("Thats my Kung Fu");
        String result = aes.encrypt("Two One Nine Twoo");
        assertEquals("29c3505f571420f6402299b31a02d73a5ce64f585e7acac4767fdfe0480d3115", result);
    }

    @Test
    public void TestDecryptPadding() {
        AesEncryption aes = new AesEncryption("Thats my Kung Fu");
        String result = aes.decrypt("29c3505f571420f6402299b31a02d73ac8b403a525da9f28f9ca9bcee9480428");
        assertEquals("Two One Nine Twoo", result);
    }

    @Test
    public void TestEncryption192() {
        AesEncryption aes = new AesEncryption("12345678password87654321", 192);
        String result = aes.encrypt("Two One Nine Two");
        assertEquals("8076212049d01a5a221ffa8794ea70490b8cac9b499697490eb5077938c324ed", result);
    }

    @Test
    public void TestDecryption192() {
        AesEncryption aes = new AesEncryption("12345678password87654321", 192);
        String result = aes.decrypt("8076212049d01a5a221ffa8794ea70490b8cac9b499697490eb5077938c324ed");
        assertEquals("Two One Nine Two", result);
    }

    @Test
    public void TestEncryption256() {
        AesEncryption aes = new AesEncryption("1234567890abcdefpassword87654321", 256);
        String result = aes.encrypt("Two One Nine Two");
        assertEquals("660afbb67ffd0645dcde7f7c918db0961d4f443e83d8cf97853970d5f610b479", result);
    }

    @Test
    public void TestDecryption256() {
        AesEncryption aes = new AesEncryption("1234567890abcdefpassword87654321", 256);
        String result = aes.decrypt("660afbb67ffd0645dcde7f7c918db0961d4f443e83d8cf97853970d5f610b479");
        assertEquals("Two One Nine Two", result);
    }
}
