# Encryption

This project implements several encryption algorithms encrypt and decrypt in Java.

Eventually, I will recreate several of the encryption algorithms to run on
and FPGA and then compare their ms.

## Encryption Algorithms

-------------------
## AES (Rijndael)
### Class AesEncryption

Uses Rijndael's algorithm to encrypt and decrypt text. Can do AES-128, AES-192 or AES-256 in ECB or in CBC with an initialization vector. See how to use the class in test/AesEncryptionTest.java

### Constructors

| Constructor and Description                                                                                                                                                                         |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **AesEncryption()**<br/> Creates an AES-128 object with a random hexadecimal key                                                                                                                    | 
| **AesEncryption(String encryptionKey)**<br/> Creates an AES-128 object with the specified key. If key is null, it will generate a random hexadecimal key                                            |
| **AesEncryption(int bits)**<br/> Creates an AES object with the specified bits. Generates a random key based on the specified bits                                                                  |
| **AesEncryption(String encryptionKey, int bits)**<br/> Creates an AES object with the specified key and bits. If key is null, it will generate a random hexadecimal key based on the specified bits |

### Method Summary
| Modifier and Type | Method and Description                                                                                                            |
|-------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| **String**        | **getKey()**<br/> returns the key used for the encryption/decryption                                                              |
| **String**        | **encrypt(String plainText)**<br/> Encrypts a string in ECB                                                                       |
| **String**        | **encrypt(String plainText, String initializationVector)**<br/> Encrypts a string in CBC via the specified initialization vector  |
| **String**        | **decrypt(String cypherText)**<br/> Decrypts a string in ECB                                                                      |
| **String**        | **decrypt(String cypherText, String initializationVector)**<br/> Decrypts a string in CBC via the specified initialization vector |

### Constructor Detail
| **AesEncryption**                                                                |
|----------------------------------------------------------------------------------|
| **AesEncryption()**<br/> Creates an AES-128 object with a random hexadecimal key | 

| **AesEncryption**                                                                                                                                                                                                                                                                                                                                           |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **AesEncryption(String encryptionKey)**<br/> Creates an AES-128 object with the specified key. If key is null, it will generate a random hexadecimal key <br/> ***Parameters:***<br/> *encryptionKey* - the hexadecimal or plain text key used to generate the round keys <br/> ***Throws:*** <br/> *IllegalArgumentException* - if the key is not 128 bits |

| **AesEncryption**                                                                                                                                                                                                                                                                |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **AesEncryption(int bits)**<br/> Creates an AES object with the specified bits. Generates a random key based on the specified bits <br/> ***Parameters:***<br/> *bits* - 128, 192, or 256 <br/> ***Throws:*** <br/> *IllegalArgumentException* - if bits is not 128, 192, or 256 |

| **AesEncryption**                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **AesEncryption(String encryptionKey, int bits)**<br/> Creates an AES object with the specified key and bits. If key is null, it will generate a random hexadecimal key based on the specified bits <br/> ***Parameters:***<br/> *encryptionKey* - the hexadecimal or plain text key used to generate the round keys<br/> *bits* - 128, 192, or 256 <br/> ***Throws:*** <br/> *IllegalArgumentException* - if the key does not correspond to the bits or if bits is not 128, 192, or 256 |

### Method Details
| public String getKey()                                                                |
|---------------------------------------------------------------------------------------|
| returns the key used for the encryption/decryption <br/> ***Returns:*** <br/> the key |

| public String encrypt(String plainText)                                                                                                    |
|--------------------------------------------------------------------------------------------------------------------------------------------|
| Encrypts a string in ECB  <br/> ***Parameters:*** <br/> *plainText* - string to be encrypted<br/>***Returns:*** <br/> the encrypted string |

| public String encrypt(String plainText, String initializationVector)                                                                                                                                                                                                                                                                                                                        |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Encrypts a string in CBC via the specified initialization vector  <br/> ***Parameters:*** <br/> *plainText* - string to be encrypted<br/> *initializationVector* - the hexadecimal or plain text initialization vector<br/> ***Throws:***<br/>*IllegalArgumentException* - if initializationVector does not correspond to a size of 128 bits<br/> ***Returns:*** <br/> the encrypted string |

| public String decrypt(String cypherText)                                                                                                                                                                                                                                   |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Decrypts a string in ECB  <br/> ***Parameters:*** <br/> *cypherText* - the hexadecimal string to be decrypted<br/>***Throws:***<br/>*IllegalArgumentException* - if cypherText is not a hexadecimal or valid encrypted text<br/> ***Returns:*** <br/> the decrypted string |

| public String decrypt(String cypherText, String initializationVector)                                                                                                                                                                                                                                                                                                                                                                                  |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Decrypts a string in CBC via the specified initialization vector  <br/> ***Parameters:*** <br/> *cypherText* - string to be decrypted<br/> *initializationVector* - the hexadecimal or plain text initialization vector<br/> ***Throws:***<br/>*IllegalArgumentException* - if initializationVector does not correspond to a size of 128 bits or cypherText is not a hexadecimal or valid encrypted text<br/>***Returns:*** <br/> the decrypted string |

### Implementation Details
1. **Rijndael's S-Box** 
   
Rijndael's S-box is generated when the class is loaded. It is generated via finding the
multiplicative inverse of each constant in GF(2). Finding the multiplicative inverse is
done brute force.

2. **rc[]**

rc is generated when the class is loaded. It is generated via rc[i] = 2 * rc[i - 1], rc[1] = 1

3. **RoundKeys**

RoundKeys are generated when the object is created. They are created in via the key string.
Each object will have exactly one set round keys.

4. **Encrypt** 

Encrypt is performed via following the order of matrix manipulation that is done during AES. 
A good outline for these steps can be found here:
https://csrc.nist.gov/csrc/media/projects/cryptographic-standards-and-guidelines/documents/aes-development/rijndael-ammended.pdf
5. **Decrypt**

Decrypt implemented by doing encrypt in reverse.


