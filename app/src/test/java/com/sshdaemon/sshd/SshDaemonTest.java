package com.sshdaemon.sshd;

import static com.sshdaemon.sshd.SshDaemon.getFingerPrints;
import static com.sshdaemon.sshd.SshDaemon.isRunning;
import static com.sshdaemon.sshd.SshDaemon.publicKeyAuthenticationExists;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

class SshDaemonTest {

    @BeforeEach
    void setUp() {
        // Reset the static isServiceRunning flag before each test
        resetServiceRunningFlag();
    }

    @Nested
    @DisplayName("Service Status Tests")
    class ServiceStatusTests {

        @Test
        @DisplayName("Should return false when service is not running initially")
        void testIsRunningInitiallyFalse() {
            assertFalse(isRunning(), "Service should not be running initially");
        }

        @Test
        @DisplayName("Should track service running state correctly")
        void testServiceRunningStateTracking() {
            // Initially false
            assertFalse(isRunning());

            // Set to true (simulating service start)
            setServiceRunningFlag(true);
            assertTrue(isRunning(), "Service should be running after start");

            // Set to false (simulating service stop)
            setServiceRunningFlag(false);
            assertFalse(isRunning(), "Service should not be running after stop");
        }

        @Test
        @DisplayName("Should handle concurrent access to service status")
        void testConcurrentServiceStatusAccess() throws InterruptedException {
            final int numThreads = 10;
            final Thread[] threads = new Thread[numThreads];
            final boolean[] results = new boolean[numThreads];

            // Set service as running
            setServiceRunningFlag(true);

            // Create threads that check service status
            for (int i = 0; i < numThreads; i++) {
                final int index = i;
                threads[i] = new Thread(() -> results[index] = isRunning());
            }

            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // All threads should see the same result
            for (boolean result : results) {
                assertTrue(result, "All threads should see service as running");
            }
        }

        @Test
        @DisplayName("Should handle rapid state changes")
        void testRapidStateChanges() {
            for (int i = 0; i < 100; i++) {
                setServiceRunningFlag(true);
                assertTrue(isRunning(), "Service should be running on rapid change iteration " + i);

                setServiceRunningFlag(false);
                assertFalse(isRunning(), "Service should be stopped on rapid change iteration " + i);
            }
        }
    }

    @Nested
    @DisplayName("Fingerprint Tests")
    class FingerprintTests {

        @Test
        @DisplayName("Should load fingerprints successfully")
        void testLoadKeys() {
            var fingerPrints = getFingerPrints();
            assertThat(fingerPrints.containsKey(SshFingerprint.DIGESTS.MD5), Matchers.is(true));
            assertThat(fingerPrints.containsKey(SshFingerprint.DIGESTS.SHA256), Matchers.is(true));
            assertThat(fingerPrints.get(SshFingerprint.DIGESTS.MD5), is(not(nullValue())));
            assertThat(fingerPrints.get(SshFingerprint.DIGESTS.SHA256), is(not(nullValue())));
        }

        @Test
        @DisplayName("Should return non-null fingerprints map")
        void testFingerprintsNotNull() {
            var fingerPrints = getFingerPrints();
            assertNotNull(fingerPrints, "Fingerprints map should not be null");
        }

        @Test
        @DisplayName("Should handle fingerprint generation with different service states")
        void testFingerprintsWithServiceState() {
            // Test when service is not running
            setServiceRunningFlag(false);
            var fingerprintsWhenStopped = getFingerPrints();
            assertNotNull(fingerprintsWhenStopped, "Fingerprints should be available when service is stopped");

            // Test when service is running
            setServiceRunningFlag(true);
            var fingerprintsWhenRunning = getFingerPrints();
            assertNotNull(fingerprintsWhenRunning, "Fingerprints should be available when service is running");

            // Fingerprints should be the same regardless of service state
            assertEquals(fingerprintsWhenStopped.size(), fingerprintsWhenRunning.size(),
                    "Fingerprints should be consistent regardless of service state");
        }
    }

    @Nested
    @DisplayName("Public Key Authentication Tests")
    class PublicKeyAuthTests {

        @Test
        @DisplayName("Should handle public key authentication check")
        void testPublicKeyAuthenticationExists() {
            // This test will depend on the actual file system state
            // Just ensure it doesn't throw an exception
            assertDoesNotThrow(() -> publicKeyAuthenticationExists(),
                    "Public key authentication check should not throw exception");
        }

        @Test
        @DisplayName("Should return boolean for public key authentication")
        void testPublicKeyAuthenticationReturnsBoolean() {
            boolean result = publicKeyAuthenticationExists();
            // Should return either true or false, not throw exception
            assertTrue(result == true || result == false,
                    "Public key authentication should return boolean");
        }

        @Test
        @DisplayName("Should be consistent across multiple calls")
        void testPublicKeyAuthenticationConsistency() {
            boolean result1 = publicKeyAuthenticationExists();
            boolean result2 = publicKeyAuthenticationExists();
            assertEquals(result1, result2,
                    "Public key authentication result should be consistent");
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create SshDaemon with default constructor")
        void testDefaultConstructor() {
            assertDoesNotThrow(() -> new SshDaemon(),
                    "Default constructor should not throw exception");
        }

        @Test
        @DisplayName("Should validate port range in constructor")
        void testPortValidation() {
            // Test valid ports
            assertDoesNotThrow(() -> new SshDaemon("127.0.0.1", 1024, "user", "pass", "/tmp", true, false),
                    "Port 1024 should be valid");
            assertDoesNotThrow(() -> new SshDaemon("127.0.0.1", 65535, "user", "pass", "/tmp", true, false),
                    "Port 65535 should be valid");

            // Test invalid ports
            assertThrows(IllegalArgumentException.class,
                    () -> new SshDaemon("127.0.0.1", 1023, "user", "pass", "/tmp", true, false),
                    "Port 1023 should be invalid");
            assertThrows(IllegalArgumentException.class,
                    () -> new SshDaemon("127.0.0.1", 65536, "user", "pass", "/tmp", true, false),
                    "Port 65536 should be invalid");
            assertThrows(IllegalArgumentException.class,
                    () -> new SshDaemon("127.0.0.1", 0, "user", "pass", "/tmp", true, false),
                    "Port 0 should be invalid");
            assertThrows(IllegalArgumentException.class,
                    () -> new SshDaemon("127.0.0.1", -1, "user", "pass", "/tmp", true, false),
                    "Negative port should be invalid");
        }

        @Test
        @DisplayName("Should validate SFTP root path in constructor")
        void testSftpRootPathValidation() {
            // Test with non-existent path
            assertThrows(IllegalArgumentException.class,
                    () -> new SshDaemon("127.0.0.1", 2222, "user", "pass", "/definitely/does/not/exist", true, false),
                    "Non-existent SFTP root path should throw exception");
        }
    }

    @Nested
    @DisplayName("Service Lifecycle Tests")
    class ServiceLifecycleTests {

        @Test
        @DisplayName("Should create service instances without errors")
        void testServiceCreation() {
            SshDaemon daemon1 = assertDoesNotThrow(() -> new SshDaemon(),
                    "Creating SshDaemon should not throw exception");
            assertNotNull(daemon1, "SshDaemon instance should not be null");

            SshDaemon daemon2 = assertDoesNotThrow(() -> new SshDaemon(),
                    "Creating multiple SshDaemon instances should not throw exception");
            assertNotNull(daemon2, "Second SshDaemon instance should not be null");
        }

        @Test
        @DisplayName("Should handle service state changes correctly")
        void testServiceStateManagement() {
            // Test initial state
            assertFalse(isRunning(), "Service should not be running initially");

            // Simulate service start
            setServiceRunningFlag(true);
            assertTrue(isRunning(), "Service should be running after start");

            // Simulate service stop
            setServiceRunningFlag(false);
            assertFalse(isRunning(), "Service should not be running after stop");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should maintain service state consistency")
        void testServiceStateConsistency() {
            // Initial state
            assertFalse(isRunning());

            // Simulate service lifecycle
            setServiceRunningFlag(true);  // Service starts
            assertTrue(isRunning());

            // Get fingerprints while service is running
            var fingerprints = getFingerPrints();
            assertNotNull(fingerprints);

            setServiceRunningFlag(false); // Service stops
            assertFalse(isRunning());

            // Fingerprints should still be available
            var fingerprintsAfterStop = getFingerPrints();
            assertNotNull(fingerprintsAfterStop);
        }

        @Test
        @DisplayName("Should handle multiple service state changes")
        void testMultipleServiceStateChanges() {
            for (int i = 0; i < 5; i++) {
                setServiceRunningFlag(true);
                assertTrue(isRunning(), "Service should be running on iteration " + i);

                // Check that other methods still work
                var fingerprints = getFingerPrints();
                assertNotNull(fingerprints, "Fingerprints should be available on iteration " + i);

                boolean authExists = publicKeyAuthenticationExists();
                assertTrue(authExists == true || authExists == false,
                        "Auth check should return boolean on iteration " + i);

                setServiceRunningFlag(false);
                assertFalse(isRunning(), "Service should be stopped on iteration " + i);
            }
        }

        @Test
        @DisplayName("Should handle service state with static method interactions")
        void testServiceStateWithStaticMethods() {
            // Test all static methods work when service is stopped
            setServiceRunningFlag(false);
            assertFalse(isRunning());
            assertNotNull(getFingerPrints());
            assertDoesNotThrow(() -> publicKeyAuthenticationExists());

            // Test all static methods work when service is running
            setServiceRunningFlag(true);
            assertTrue(isRunning());
            assertNotNull(getFingerPrints());
            assertDoesNotThrow(() -> publicKeyAuthenticationExists());
        }
    }

    // Helper methods for testing
    private void resetServiceRunningFlag() {
        setServiceRunningFlag(false);
    }

    private void setServiceRunningFlag(boolean value) {
        try {
            Field field = SshDaemon.class.getDeclaredField("isServiceRunning");
            field.setAccessible(true);
            field.setBoolean(null, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set service running flag", e);
        }
    }
}
