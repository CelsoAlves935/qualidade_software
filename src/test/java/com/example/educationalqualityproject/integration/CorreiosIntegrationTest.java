package com.example.educationalqualityproject.integration;

import com.example.educationalqualityproject.entity.Address;
import com.example.educationalqualityproject.entity.Student;
import com.example.educationalqualityproject.repository.StudentRepository;
import com.example.educationalqualityproject.service.CorreiosService;
import com.example.educationalqualityproject.util.VcrHelper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating VCR pattern with TestContainers.
 * 
 * This test shows how to:
 * 1. Use TestContainers for MongoDB integration testing (LOCAL ONLY)
 * 2. Use VCR pattern for mocking external HTTP API calls (Correios CEP lookup)
 * 3. Combine both approaches in a single integration test
 * 
 * NOTE: TestContainers are used ONLY in local development.
 * In CI, we use only VCR mocks (no TestContainers).
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CorreiosIntegrationTest {

    // TestContainers - MongoDB for local integration testing
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8");

    @Autowired
    private StudentRepository studentRepository;

    private VcrHelper vcrHelper;
    private CorreiosService correiosService;

    @BeforeAll
    static void setUpContainer() {
        // TestContainers starts the MongoDB container automatically
        System.out.println("TestContainers: Starting MongoDB container...");
    }

    @AfterAll
    static void tearDownContainer() {
        // TestContainers stops the MongoDB container automatically
        System.out.println("TestContainers: Stopping MongoDB container...");
    }

    @BeforeEach
    void setUp() {
        // Clean database before each test
        studentRepository.deleteAll();
        
        // Initialize VCR helper for playback mode
        // In record mode, you would set: new VcrHelper("path", "name", true)
        vcrHelper = new VcrHelper("src/test/resources/vcr-cassettes", "cep_01001000", false);
        
        try {
            vcrHelper.start();
            
            // Create CorreiosService with mocked HTTP client
            OkHttpClient mockClient = vcrHelper.getOkHttpClient();
            correiosService = new CorreiosService(mockClient);
            
        } catch (Exception e) {
            fail("Failed to initialize VCR helper: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (vcrHelper != null) {
            vcrHelper.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should find address by CEP using VCR playback")
    void testFindAddressByCepWithVcr() throws IOException {
        // Given: VCR will playback the recorded response for CEP 01001000
        String cep = "01001000";

        // When: We call the Correios service
        Address address = correiosService.findAddressByCep(cep);

        // Then: We should get the recorded address from the cassette
        assertNotNull(address);
        assertEquals("01001-000", address.getCep());
        assertEquals("Praça da Sé", address.getLogradouro());
        assertEquals("lado ímpar", address.getComplemento());
        assertEquals("Sé", address.getBairro());
        assertEquals("São Paulo", address.getLocalidade());
        assertEquals("SP", address.getUf());
        assertEquals("1004", address.getGia());
        assertEquals("11", address.getDdd());
    }

    @Test
    @Order(2)
    @DisplayName("Should save student with address from CEP lookup")
    void testSaveStudentWithAddressFromCep() throws IOException {
        // Given: A student with address information from CEP lookup
        String cep = "01001000";
        Address address = correiosService.findAddressByCep(cep);

        Student student = new Student(
                "João Silva",
                "joao.silva@example.com",
                "2024001"
        );

        // When: We save the student in MongoDB (using TestContainers)
        Student savedStudent = studentRepository.save(student);

        // Then: Student should be persisted with MongoDB
        assertNotNull(savedStudent.getId());
        assertEquals("João Silva", savedStudent.getName());
        assertEquals("joao.silva@example.com", savedStudent.getEmail());
        assertEquals("2024001", savedStudent.getRegistrationNumber());
        assertNotNull(savedStudent.getCreatedAt());
        assertNotNull(savedStudent.getUpdatedAt());

        // Verify address was retrieved successfully via VCR
        assertNotNull(address);
        assertEquals("São Paulo", address.getLocalidade());
        assertEquals("SP", address.getUf());
    }

    @Test
    @Order(3)
    @DisplayName("Should handle invalid CEP with VCR playback")
    void testInvalidCepWithVcr() {
        // Given: VCR will playback error response for invalid CEP
        VcrHelper errorVcr = new VcrHelper("src/test/resources/vcr-cassettes", "cep_invalid", false);
        
        try {
            errorVcr.start();
            CorreiosService errorService = new CorreiosService(errorVcr.getOkHttpClient());

            // When/Then: Should throw exception for invalid CEP
            assertThrows(IOException.class, () -> {
                errorService.findAddressByCep("00000000");
            });
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        } finally {
            try {
                errorVcr.stop();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should verify student count in MongoDB after multiple saves")
    void testMultipleStudentsInMongoDb() throws IOException {
        // Given: Multiple students with different CEPs
        String cep1 = "01001000"; // São Paulo
        Address address1 = correiosService.findAddressByCep(cep1);

        Student student1 = new Student("Maria Santos", "maria@example.com", "2024002");
        Student student2 = new Student("Pedro Oliveira", "pedro@example.com", "2024003");

        // When: We save multiple students
        studentRepository.save(student1);
        studentRepository.save(student2);

        // Then: MongoDB should have exactly 2 students
        long count = studentRepository.count();
        assertEquals(2, count);

        // Verify we can retrieve them
        assertTrue(studentRepository.existsByEmail("maria@example.com"));
        assertTrue(studentRepository.existsByEmail("pedro@example.com"));

        // Verify VCR returned valid address
        assertNotNull(address1);
        assertEquals("Praça da Sé", address1.getLogradouro());
    }

    @Test
    @Order(5)
    @DisplayName("Should demonstrate VCR recording pattern (documentation)")
    void testVcrRecordingPatternDocumentation() {
        // This test documents how to record new VCR cassettes
        // 
        // To RECORD a new cassette:
        // 1. Set recordMode = true in VcrHelper constructor
        // 2. Make real API calls to the external service
        // 3. VcrHelper will save the interactions to a JSON file
        // 4. Set recordMode = false to use playback mode
        //
        // Example code for recording:
        // VcrHelper recorder = new VcrHelper("src/test/resources/vcr-cassettes", 
        //                                    "new_cassette_name", true);
        // recorder.start();
        // // Make real API calls here
        // Address address = correiosService.findAddressByCep("70002900");
        // recorder.stop();
        //
        // The cassette file will contain the HTTP interaction
        // and can be used for playback in future tests

        assertTrue(true, "This test documents VCR recording pattern");
    }
}
