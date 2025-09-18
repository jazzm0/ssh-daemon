package com.sshdaemon.sshd;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

class SshPublicKeyAuthenticatorTest {

    private SshPublicKeyAuthenticator sshPublicKeyAuthenticator;

    @BeforeEach
    void setUp() {
        sshPublicKeyAuthenticator = new SshPublicKeyAuthenticator();
    }

    @Nested
    @DisplayName("Key Loading Tests")
    class KeyLoadingTests {

        @Test
        @DisplayName("Should load RSA key correctly")
        void testLoadRSAKey() throws Exception {
            var rsaPublicKey = (RSAPublicKey) SshPublicKeyAuthenticator.readKey("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCrkf5RHFcmmnPFxfOVsVOCdDVfs04dZg+/n808/NEdyOPuyAde4UIvZbzKEjW9brtEvOHCFfxZuXa0TbTIUau9p+4gWTGXIONcarwJ7LtNUlWfJiWYmIWVgyNnpzVftcW3mi8gRGxPbCJM2yVeB7gv452wvWPDe9TFdpgbwhLBqVIRG6EBHC0VBXX8qKNCbFoclYbiXa5DfwMkxYwN2yyKaSu75e0H4FP4BehaqQ6SfBIThqQRVdcx9J9Du3GzTi4ArN0timPAQ+X17pWxgEQ3qNbj49Lnteu+NSmb0PawcrP+Ykd7oy82kXm/hRM6cLjS1GOTsXpGDFf0NevAW8b3 D050150@WDFL34195932A");
            assertThat(rsaPublicKey.getPublicExponent(), is(new BigInteger("65537")));
            assertThat(rsaPublicKey.getModulus(), is(new BigInteger("21658742190318166967712730864679652658650859121969481181201380769435852715982079838796135745206268981260737877360141273622280512537469661232310601414632396577736750997307043633989350470146139654498683603607823966490835477269345553397205866827412911445557084380501015516582017566897110095005407768881980022943053565933828297090533987425102831869390057642704253755269803136323388759627399370507151238064778399477125470941103468997107204954580888346976963732529191611522789249471940599415587667163136427455499142265843852906870573003016543761403915579728832278943756709241709719567708592405407294409003276217649490282231")));
        }

        @Test
        @DisplayName("Should load ED25519 key correctly")
        void testLoadED25519Key() throws Exception {
            var publicKey = SshPublicKeyAuthenticator.readKey("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIGJ0j5BztROLdZYHf8cpJsJr9jd8gCRUfm6oe9k3Bhh0 @quantenzitrone:matrix.org");
            assertThat(publicKey.getFormat(), is("X.509"));
            assertThat(publicKey.getEncoded(), is(new byte[]{48, 42, 48, 5, 6, 3, 43, 101, 112, 3, 33, 0, 98, 116, -113, -112, 115, -75, 19, -117, 117, -106, 7, 127, -57, 41, 38, -62, 107, -10, 55, 124, -128, 36, 84, 126, 110, -88, 123, -39, 55, 6, 24, 116}));
        }

        @Test
        @DisplayName("Should load keys from file path")
        void testLoadKeyFromFilePath() throws Exception {
            var resourceDirectory = Paths.get("src", "test", "resources");
            var absolutePath = resourceDirectory.toFile().getAbsolutePath() + "/authorized_keys";
            assertTrue(sshPublicKeyAuthenticator.loadKeysFromPath(absolutePath));
            var authorizedKeys = sshPublicKeyAuthenticator.getAuthorizedKeys();
            assertThat(authorizedKeys.size(), is(3));

            var exponent = new BigInteger("65537");
            var firstKey = new RSAPublicKeySpec(new BigInteger("21658742190318166967712730864679652658650859121969481181201380769435852715982079838796135745206268981260737877360141273622280512537469661232310601414632396577736750997307043633989350470146139654498683603607823966490835477269345553397205866827412911445557084380501015516582017566897110095005407768881980022943053565933828297090533987425102831869390057642704253755269803136323388759627399370507151238064778399477125470941103468997107204954580888346976963732529191611522789249471940599415587667163136427455499142265843852906870573003016543761403915579728832278943756709241709719567708592405407294409003276217649490282231"), exponent);
            var secondKey = new RSAPublicKeySpec(new BigInteger("784057767550419878369497798651827476361889178814477573346641631234935186314436782078536135459192115951182846131604763290106347820711735919818466318881966145658619187844260657686465054388415729621256835109072466122751680324465523571218314239699133938274929722422531435916124040593004728158303703638151544229751515190620733194729793182402256827874540802963173001942073095959874409030457157131068008004452131416339302414300154381574660775550756346290523471370004641759457082400056951523140192837676235596014868691294116723696798672826048372197524626777597698825985438359440849049188507660150181938186465442057503356737043772475149570597456464086884083587865261320028768112850655672995490391301788008160746624607620536612729945152345637233101657918767620370276196646289217228948026575176667526067692435995447599542086540447642569281636925038610129227622664311974763550767950513197666055242104878427773497759504733742315824234665981633069516518731571901751350961661458615098275390788530389016622885595867572513280042783815166138155280065655067579686735407066393737630455891385113394433769584091954362175665925155421775122038568449049307648037245144977590866670732267987944408567171290527382426393459497954986775620782288149569534834718157"), exponent);
            var keyFactory = KeyFactory.getInstance("RSA");
            var ed25519Key = SshPublicKeyAuthenticator.readKey("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIGJ0j5BztROLdZYHf8cpJsJr9jd8gCRUfm6oe9k3Bhh0 @quantenzitrone:matrix.org");
            assertThat(authorizedKeys, containsInAnyOrder(
                    keyFactory.generatePublic(firstKey),
                    keyFactory.generatePublic(secondKey),
                    ed25519Key));
        }

        @Test
        @DisplayName("Should handle DSA key loading failure")
        void testLoadDSAKeyFromFilePath() {
            var resourceDirectory = Paths.get("src", "test", "resources");
            var absolutePath = resourceDirectory.toFile().getAbsolutePath() + "/id_dsa.pub";
            assertFalse(sshPublicKeyAuthenticator.loadKeysFromPath(absolutePath));

            var authorizedKeys = sshPublicKeyAuthenticator.getAuthorizedKeys();
            assertThat(authorizedKeys.isEmpty(), is(true));
        }

        @Test
        @DisplayName("Should handle invalid key content")
        void testLoadInvalidKeyContent() throws Exception {
            var resourceDirectory = Paths.get("src", "test", "resources");
            var absolutePath = resourceDirectory.toFile().getAbsolutePath() + "/invalid_authorized_keys";
            assertFalse(sshPublicKeyAuthenticator.loadKeysFromPath(absolutePath));
            var authorizedKeys = sshPublicKeyAuthenticator.getAuthorizedKeys();
            assertThat(authorizedKeys.isEmpty(), is(true));
        }

        @Test
        @DisplayName("Should handle non-existent file path")
        void testLoadFromNonExistentPath() {
            String nonExistentPath = "/path/that/does/not/exist/authorized_keys";
            assertFalse(sshPublicKeyAuthenticator.loadKeysFromPath(nonExistentPath));
            var authorizedKeys = sshPublicKeyAuthenticator.getAuthorizedKeys();
            assertThat(authorizedKeys, is(empty()));
        }

        @Test
        @DisplayName("Should handle null file path")
        void testLoadFromNullPath() {
            assertFalse(sshPublicKeyAuthenticator.loadKeysFromPath(null));
            var authorizedKeys = sshPublicKeyAuthenticator.getAuthorizedKeys();
            assertThat(authorizedKeys, is(empty()));
        }

        @Test
        @DisplayName("Should handle empty file path")
        void testLoadFromEmptyPath() {
            assertFalse(sshPublicKeyAuthenticator.loadKeysFromPath(""));
            var authorizedKeys = sshPublicKeyAuthenticator.getAuthorizedKeys();
            assertThat(authorizedKeys, is(empty()));
        }
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should authenticate with different key types")
        void testAuthenticationWithDifferentKeys() throws Exception {
            var resourceDirectory = Paths.get("src", "test", "resources");
            var absolutePath = resourceDirectory.toFile().getAbsolutePath() + "/authorized_keys";
            assertTrue(sshPublicKeyAuthenticator.loadKeysFromPath(absolutePath));

            var exponent = new BigInteger("65537");
            var firstKey = new RSAPublicKeySpec(new BigInteger("21658742190318166967712730864679652658650859121969481181201380769435852715982079838796135745206268981260737877360141273622280512537469661232310601414632396577736750997307043633989350470146139654498683603607823966490835477269345553397205866827412911445557084380501015516582017566897110095005407768881980022943053565933828297090533987425102831869390057642704253755269803136323388759627399370507151238064778399477125470941103468997107204954580888346976963732529191611522789249471940599415587667163136427455499142265843852906870573003016543761403915579728832278943756709241709719567708592405407294409003276217649490282231"), exponent);
            var secondKey = new RSAPublicKeySpec(new BigInteger("784057767550419878369497798651827476361889178814477573346641631234935186314436782078536135459192115951182846131604763290106347820711735919818466318881966145658619187844260657686465054388415729621256835109072466122751680324465523571218314239699133938274929722422531435916124040593004728158303703638151544229751515190620733194729793182402256827874540802963173001942073095959874409030457157131068008004452131416339302414300154381574660775550756346290523471370004641759457082400056951523140192837676235596014868691294116723696798672826048372197524626777597698825985438359440849049188507660150181938186465442057503356737043772475149570597456464086884083587865261320028768112850655672995490391301788008160746624607620536612729945152345637233101657918767620370276196646289217228948026575176667526067692435995447599542086540447642569281636925038610129227622664311974763550767950513197666055242104878427773497759504733742315824234665981633069516518731571901751350961661458615098275390788530389016622885595867572513280042783815166138155280065655067579686735407066393737630455891385113394433769584091954362175665925155421775122038568449049307648037245144977590866670732267987944408567171290527382426393459497954986775620782288149569534834718157"), exponent);
            var unauthorizedKey = new RSAPublicKeySpec(new BigInteger("666057767550419878369497798651827476361889178814477573346641631234935186314436782078536135459192115951182846131604763290106347820711735919818466318881966145658619187844260657686465054388415729621256835109072466122751680324465523571218314239699133938274929722422531435916124040593004728158303703638151544229751515190620733194729793182402256827874540802963173001942073095959874409030457157131068008004452131416339302414300154381574660775550756346290523471370004641759457082400056951523140192837676235596014868691294116723696798672826048372197524626777597698825985438359440849049188507660150181938186465442057503356737043772475149570597456464086884083587865261320028768112850655672995490391301788008160746624607620536612729945152345637233101657918767620370276196646289217228948026575176667526067692435995447599542086540447642569281636925038610129227622664311974763550767950513197666055242104878427773497759504733742315824234665981633069516518731571901751350961661458615098275390788530389016622885595867572513280042783815166138155280065655067579686735407066393737630455891385113394433769584091954362175665925155421775122038568449049307648037245144977590866670732267987944408567171290527382426393459497954986775620782288149569534834718666"), exponent);
            var keyFactory = KeyFactory.getInstance("RSA");

            var ed25519Key = SshPublicKeyAuthenticator.readKey("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIGJ0j5BztROLdZYHf8cpJsJr9jd8gCRUfm6oe9k3Bhh0 @quantenzitrone:matrix.org");

            // Test authorized keys
            assertThat(sshPublicKeyAuthenticator.authenticate(null, keyFactory.generatePublic(firstKey), null), is(true));
            assertThat(sshPublicKeyAuthenticator.authenticate(null, keyFactory.generatePublic(secondKey), null), is(true));
            assertThat(sshPublicKeyAuthenticator.authenticate(null, ed25519Key, null), is(true));

            // Test unauthorized key
            assertThat(sshPublicKeyAuthenticator.authenticate(null, keyFactory.generatePublic(unauthorizedKey), null), is(false));
        }

        @Test
        @DisplayName("Should handle authentication with null key")
        void testAuthenticationWithNullKey() {
            var resourceDirectory = Paths.get("src", "test", "resources");
            var absolutePath = resourceDirectory.toFile().getAbsolutePath() + "/authorized_keys";
            assertTrue(sshPublicKeyAuthenticator.loadKeysFromPath(absolutePath));

            assertThat(sshPublicKeyAuthenticator.authenticate(null, null, null), is(false));
        }

        @Test
        @DisplayName("Should handle session parameter correctly")
        void testAuthenticationWithSession() throws Exception {
            var resourceDirectory = Paths.get("src", "test", "resources");
            var absolutePath = resourceDirectory.toFile().getAbsolutePath() + "/authorized_keys";
            assertTrue(sshPublicKeyAuthenticator.loadKeysFromPath(absolutePath));

            var exponent = new BigInteger("65537");
            var firstKey = new RSAPublicKeySpec(new BigInteger("21658742190318166967712730864679652658650859121969481181201380769435852715982079838796135745206268981260737877360141273622280512537469661232310601414632396577736750997307043633989350470146139654498683603607823966490835477269345553397205866827412911445557084380501015516582017566897110095005407768881980022943053565933828297090533987425102831869390057642704253755269803136323388759627399370507151238064778399477125470941103468997107204954580888346976963732529191611522789249471940599415587667163136427455499142265843852906870573003016543761403915579728832278943756709241709719567708592405407294409003276217649490282231"), exponent);
            var keyFactory = KeyFactory.getInstance("RSA");

            // Test with null session (which is what we pass in other tests)
            assertThat(sshPublicKeyAuthenticator.authenticate(null, keyFactory.generatePublic(firstKey), null), is(true));
        }
    }

    @Nested
    @DisplayName("Key Management Tests")
    class KeyManagementTests {

        @Test
        @DisplayName("Should return empty list initially")
        void testInitialKeyList() {
            var keys = sshPublicKeyAuthenticator.getAuthorizedKeys();
            assertThat(keys, is(empty()));
        }

        @Test
        @DisplayName("Should maintain key list after loading")
        void testKeyListAfterLoading() {
            var resourceDirectory = Paths.get("src", "test", "resources");
            var absolutePath = resourceDirectory.toFile().getAbsolutePath() + "/authorized_keys";

            assertTrue(sshPublicKeyAuthenticator.loadKeysFromPath(absolutePath));
            var keys = sshPublicKeyAuthenticator.getAuthorizedKeys();
            assertThat(keys, hasSize(3));
            assertNotNull(keys);
        }

        @Test
        @DisplayName("Should clear keys on failed load")
        void testKeysClearedOnFailedLoad() {
            // First load valid keys
            var resourceDirectory = Paths.get("src", "test", "resources");
            var absolutePath = resourceDirectory.toFile().getAbsolutePath() + "/authorized_keys";
            assertTrue(sshPublicKeyAuthenticator.loadKeysFromPath(absolutePath));
            assertThat(sshPublicKeyAuthenticator.getAuthorizedKeys(), hasSize(3));

            // Then try to load invalid keys
            var invalidPath = resourceDirectory.toFile().getAbsolutePath() + "/invalid_authorized_keys";
            assertFalse(sshPublicKeyAuthenticator.loadKeysFromPath(invalidPath));
            assertThat(sshPublicKeyAuthenticator.getAuthorizedKeys(), is(empty()));
        }
    }

    @Nested
    @DisplayName("Key Format Tests")
    class KeyFormatTests {

        @Test
        @DisplayName("Should handle malformed key strings")
        void testMalformedKeyStrings() {
            assertDoesNotThrow(() -> SshPublicKeyAuthenticator.readKey("invalid-key-format"));
            assertDoesNotThrow(() -> SshPublicKeyAuthenticator.readKey("ssh-rsa"));
            assertDoesNotThrow(() -> SshPublicKeyAuthenticator.readKey("ssh-rsa invalid-base64"));
            assertDoesNotThrow(() -> SshPublicKeyAuthenticator.readKey(""));
        }

        @Test
        @DisplayName("Should handle keys with comments")
        void testKeysWithComments() {
            String keyWithComment = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCrkf5RHFcmmnPFxfOVsVOCdDVfs04dZg+/n808/NEdyOPuyAde4UIvZbzKEjW9brtEvOHCFfxZuXa0TbTIUau9p+4gWTGXIONcarwJ7LtNUlWfJiWYmIWVgyNnpzVftcW3mi8gRGxPbCJM2yVeB7gv452wvWPDe9TFdpgbwhLBqVIRG6EBHC0VBXX8qKNCbFoclYbiXa5DfwMkxYwN2yyKaSu75e0H4FP4BehaqQ6SfBIThqQRVdcx9J9Du3GzTi4ArN0timPAQ+X17pWxgEQ3qNbj49Lnteu+NSmb0PawcrP+Ykd7oy82kXm/hRM6cLjS1GOTsXpGDFf0NevAW8b3 user@hostname";

            assertDoesNotThrow(() -> SshPublicKeyAuthenticator.readKey(keyWithComment));
        }
    }

    @Nested
    @DisplayName("File Operations Tests")
    class FileOperationsTests {

        @Test
        @DisplayName("Should handle temporary files correctly")
        void testTemporaryFileHandling(@TempDir Path tempDir) throws IOException {
            // Create a temporary authorized_keys file
            Path tempKeyFile = tempDir.resolve("temp_authorized_keys");
            String keyContent = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCrkf5RHFcmmnPFxfOVsVOCdDVfs04dZg+/n808/NEdyOPuyAde4UIvZbzKEjW9brtEvOHCFfxZuXa0TbTIUau9p+4gWTGXIONcarwJ7LtNUlWfJiWYmIWVgyNnpzVftcW3mi8gRGxPbCJM2yVeB7gv452wvWPDe9TFdpgbwhLBqVIRG6EBHC0VBXX8qKNCbFoclYbiXa5DfwMkxYwN2yyKaSu75e0H4FP4BehaqQ6SfBIThqQRVdcx9J9Du3GzTi4ArN0timPAQ+X17pWxgEQ3qNbj49Lnteu+NSmb0PawcrP+Ykd7oy82kXm/hRM6cLjS1GOTsXpGDFf0NevAW8b3 test@example.com\n";
            Files.write(tempKeyFile, keyContent.getBytes());

            assertTrue(sshPublicKeyAuthenticator.loadKeysFromPath(tempKeyFile.toString()));
            assertThat(sshPublicKeyAuthenticator.getAuthorizedKeys(), hasSize(1));
        }

        @Test
        @DisplayName("Should handle empty files")
        void testEmptyFile(@TempDir Path tempDir) throws IOException {
            Path emptyKeyFile = tempDir.resolve("empty_authorized_keys");
            Files.write(emptyKeyFile, "".getBytes());

            assertFalse(sshPublicKeyAuthenticator.loadKeysFromPath(emptyKeyFile.toString()));
            assertThat(sshPublicKeyAuthenticator.getAuthorizedKeys(), is(empty()));
        }

        @Test
        @DisplayName("Should handle files with only whitespace")
        void testWhitespaceOnlyFile(@TempDir Path tempDir) throws IOException {
            Path whitespaceKeyFile = tempDir.resolve("whitespace_authorized_keys");
            Files.write(whitespaceKeyFile, "   \n\t\n   ".getBytes());

            assertFalse(sshPublicKeyAuthenticator.loadKeysFromPath(whitespaceKeyFile.toString()));
            assertThat(sshPublicKeyAuthenticator.getAuthorizedKeys(), is(empty()));
        }

        @Test
        @DisplayName("Should handle mixed valid and invalid keys")
        void testMixedValidInvalidKeys(@TempDir Path tempDir) throws IOException {
            Path mixedKeyFile = tempDir.resolve("mixed_authorized_keys");
            String mixedContent = """
                    ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCrkf5RHFcmmnPFxfOVsVOCdDVfs04dZg+/n808/NEdyOPuyAde4UIvZbzKEjW9brtEvOHCFfxZuXa0TbTIUau9p+4gWTGXIONcarwJ7LtNUlWfJiWYmIWVgyNnpzVftcW3mi8gRGxPbCJM2yVeB7gv452wvWPDe9TFdpgbwhLBqVIRG6EBHC0VBXX8qKNCbFoclYbiXa5DfwMkxYwN2yyKaSu75e0H4FP4BehaqQ6SfBIThqQRVdcx9J9Du3GzTi4ArN0timPAQ+X17pWxgEQ3qNbj49Lnteu+NSmb0PawcrP+Ykd7oy82kXm/hRM6cLjS1GOTsXpGDFf0NevAW8b3 valid@example.com
                    invalid-key-line
                    ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIGJ0j5BztROLdZYHf8cpJsJr9jd8gCRUfm6oe9k3Bhh0 @quantenzitrone:matrix.org
                    another-invalid-line
                    """;
            Files.write(mixedKeyFile, mixedContent.getBytes());

            assertTrue(sshPublicKeyAuthenticator.loadKeysFromPath(mixedKeyFile.toString()));
            // Should load only the valid keys
            assertThat(sshPublicKeyAuthenticator.getAuthorizedKeys(), hasSize(2));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle corrupted key files gracefully")
        void testCorruptedKeyFile() {
            var resourceDirectory = Paths.get("src", "test", "resources");
            var absolutePath = resourceDirectory.toFile().getAbsolutePath() + "/invalid_authorized_keys";

            // Should not throw exception, just return false
            assertDoesNotThrow(() -> sshPublicKeyAuthenticator.loadKeysFromPath(absolutePath));
            assertFalse(sshPublicKeyAuthenticator.loadKeysFromPath(absolutePath));
        }

        @Test
        @DisplayName("Should handle directory instead of file")
        void testDirectoryPath() {
            var resourceDirectory = Paths.get("src", "test", "resources");
            var directoryPath = resourceDirectory.toFile().getAbsolutePath();

            assertFalse(sshPublicKeyAuthenticator.loadKeysFromPath(directoryPath));
            assertThat(sshPublicKeyAuthenticator.getAuthorizedKeys(), is(empty()));
        }

        @Test
        @DisplayName("Should handle very long file paths")
        void testVeryLongPath() {
            StringBuilder longPath = new StringBuilder("/");
            for (int i = 0; i < 1000; i++) {
                longPath.append("very_long_directory_name_").append(i).append("/");
            }
            longPath.append("authorized_keys");

            assertFalse(sshPublicKeyAuthenticator.loadKeysFromPath(longPath.toString()));
            assertThat(sshPublicKeyAuthenticator.getAuthorizedKeys(), is(empty()));
        }
    }
}
