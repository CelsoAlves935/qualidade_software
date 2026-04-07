package com.example.educationalqualityproject.vcr;

import com.example.educationalqualityproject.entity.Address;
import com.example.educationalqualityproject.service.CorreiosService;
import com.example.educationalqualityproject.util.VcrHelper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VCR-only tests for Correios CEP lookup.
 * 
 * These tests use MockWebServer to playback recorded HTTP interactions.
 * No external network calls are made - all responses come from cassette files.
 * 
 * This test suite is safe to run in CI because:
 * - No external dependencies (no real API calls)
 * - No TestContainers required
 * - Fast execution (milliseconds)
 * - Deterministic results (same cassettes every time)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CorreiosVcrTest {

    private VcrHelper vcrHelper;
    private CorreiosService correiosService;

    @AfterEach
    void tearDown() throws IOException {
        if (vcrHelper != null) {
            vcrHelper.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should find address for valid CEP 01001000 (São Paulo)")
    void testFindAddressByCep_SaoPaulo() throws IOException {
        // Given: VCR playback of CEP 01001000
        vcrHelper = new VcrHelper("src/test/resources/vcr-cassettes", "cep_01001000", false);
        vcrHelper.start();
        
        OkHttpClient mockClient = vcrHelper.getOkHttpClient();
        correiosService = new CorreiosService(mockClient);

        // When: We lookup the CEP
        Address address = correiosService.findAddressByCep("01001000");

        // Then: Should return the recorded address
        assertNotNull(address);
        assertEquals("01001-000", address.getCep());
        assertEquals("Praça da Sé", address.getLogradouro());
        assertEquals("lado ímpar", address.getComplemento());
        assertEquals("Sé", address.getBairro());
        assertEquals("São Paulo", address.getLocalidade());
        assertEquals("SP", address.getUf());
        assertEquals("3550308", address.getIbge());
        assertEquals("1004", address.getGia());
        assertEquals("11", address.getDdd());
        assertEquals("7107", address.getSiafi());
    }

    @Test
    @Order(2)
    @DisplayName("Should find address for valid CEP 20040020 (Rio de Janeiro)")
    void testFindAddressByCep_RioDeJaneiro() throws IOException {
        // Given: VCR playback of CEP 20040020
        vcrHelper = new VcrHelper("src/test/resources/vcr-cassettes", "cep_20040020", false);
        vcrHelper.start();
        
        OkHttpClient mockClient = vcrHelper.getOkHttpClient();
        correiosService = new CorreiosService(mockClient);

        // When: We lookup the CEP
        Address address = correiosService.findAddressByCep("20040020");

        // Then: Should return the recorded address
        assertNotNull(address);
        assertEquals("20040-020", address.getCep());
        assertEquals("Praça Pio X", address.getLogradouro());
        assertEquals("lado ímpar", address.getComplemento());
        assertEquals("Centro", address.getBairro());
        assertEquals("Rio de Janeiro", address.getLocalidade());
        assertEquals("RJ", address.getUf());
        assertEquals("3304557", address.getIbge());
        assertEquals("21", address.getDdd());
    }

    @Test
    @Order(3)
    @DisplayName("Should handle invalid CEP that returns error")
    void testFindAddressByCep_Invalid() {
        // Given: VCR playback of invalid CEP
        vcrHelper = new VcrHelper("src/test/resources/vcr-cassettes", "cep_invalid", false);
        
        try {
            vcrHelper.start();
            OkHttpClient mockClient = vcrHelper.getOkHttpClient();
            correiosService = new CorreiosService(mockClient);

            // When/Then: Should throw IOException for invalid CEP
            assertThrows(IOException.class, () -> {
                correiosService.findAddressByCep("00000000");
            }, "Should throw exception for CEP that returns error");
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        } finally {
            try {
                vcrHelper.stop();
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should reject CEP with invalid format (less than 8 digits)")
    void testFindAddressByCep_InvalidFormat_Short() {
        // Given: Service with any HTTP client
        vcrHelper = new VcrHelper("src/test/resources/vcr-cassettes", "cep_01001000", false);
        
        try {
            vcrHelper.start();
            OkHttpClient mockClient = vcrHelper.getOkHttpClient();
            correiosService = new CorreiosService(mockClient);

            // When/Then: Should reject CEP with wrong format
            assertThrows(IllegalArgumentException.class, () -> {
                correiosService.findAddressByCep("1234567"); // Only 7 digits
            }, "Should reject CEP with less than 8 digits");
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        } finally {
            try {
                vcrHelper.stop();
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should reject CEP with invalid format (more than 8 digits)")
    void testFindAddressByCep_InvalidFormat_Long() {
        // Given: Service with any HTTP client
        vcrHelper = new VcrHelper("src/test/resources/vcr-cassettes", "cep_01001000", false);
        
        try {
            vcrHelper.start();
            OkHttpClient mockClient = vcrHelper.getOkHttpClient();
            correiosService = new CorreiosService(mockClient);

            // When/Then: Should reject CEP with wrong format
            assertThrows(IllegalArgumentException.class, () -> {
                correiosService.findAddressByCep("123456789"); // 9 digits
            }, "Should reject CEP with more than 8 digits");
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        } finally {
            try {
                vcrHelper.stop();
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should handle CEP with formatting (dash)")
    void testFindAddressByCep_WithDash() throws IOException {
        // Given: VCR playback
        vcrHelper = new VcrHelper("src/test/resources/vcr-cassettes", "cep_01001000", false);
        vcrHelper.start();
        
        OkHttpClient mockClient = vcrHelper.getOkHttpClient();
        correiosService = new CorreiosService(mockClient);

        // When: We lookup CEP with dash formatting
        Address address = correiosService.findAddressByCep("01001-000");

        // Then: Should clean the formatting and return address
        assertNotNull(address);
        assertEquals("01001-000", address.getCep());
        assertEquals("Praça da Sé", address.getLogradouro());
    }

    @Test
    @Order(7)
    @DisplayName("Should demonstrate VCR benefits for CI/CD")
    void testVcrBenefitsDocumentation() {
        // This test documents the benefits of VCR pattern:
        //
        // VCR Pattern Benefits:
        // 1. SPEED: Tests run in milliseconds (no network latency)
        // 2. RELIABILITY: No dependency on external API availability
        // 3. DETERMINISTIC: Same response every time
        // 4. OFFLINE: Tests work without internet connection
        // 5. CI-FRIENDLY: No need for TestContainers in CI
        // 6. COST-EFFECTIVE: No API rate limiting concerns
        //
        // When to use VCR:
        // - Testing external API integrations
        // - Third-party service calls (payment gateways, email services, etc.)
        // - Web service clients
        // - Any HTTP-based integration
        //
        // When NOT to use VCR:
        // - Testing database interactions (use TestContainers)
        // - Testing internal service communication
        // - Testing real-time data requirements

        assertTrue(true, "VCR pattern is ideal for CI/CD pipelines");
    }
}
