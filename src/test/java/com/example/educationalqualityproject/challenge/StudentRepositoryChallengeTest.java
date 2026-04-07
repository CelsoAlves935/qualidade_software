package com.example.educationalqualityproject.challenge;

import com.example.educationalqualityproject.entity.Student;
import com.example.educationalqualityproject.repository.StudentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🚨 DESAFIO PARA O ALUNO - TESTE QUEBRADO! 🚨
 * 
 * Este teste está quebrado e não vai passar.
 * Sua missão: identificar e corrigir os problemas!
 * 
 * DICAS:
 * 1. O teste usa TestContainers para subir um MongoDB real
 * 2. Existem ERROS neste arquivo que impedem os testes de passar
 * 3. Leia as mensagens de erro cuidadosamente
 * 4. Verifique se as configurações estão corretas
 * 
 * OBJETIVO: Fazer todos os testes deste arquivo passarem!
 * 
 * Boa sorte! 🚀
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StudentRepositoryChallengeTest {

    // ❌ ERRO 1: Container configurado errado
    // DICA: Verifique a versão da imagem do MongoDB
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:99");

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    void setUp() {
        // Limpa o banco antes de cada teste
        studentRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Deve salvar um estudante no banco")
    void testSaveStudent() {
        // Given
        Student student = new Student(
            "Maria Silva",
            "maria.silva@email.com",
            "2024001"
        );

        // When
        Student savedStudent = studentRepository.save(student);

        // Then
        assertNotNull(savedStudent.getId());
        assertEquals("Maria Silva", savedStudent.getName());
        assertEquals("maria.silva@email.com", savedStudent.getEmail());
        assertEquals("2024001", savedStudent.getRegistrationNumber());
    }

    @Test
    @Order(2)
    @DisplayName("Deve buscar estudante por ID")
    void testFindStudentById() {
        // Given
        Student student = new Student("João Santos", "joao@email.com", "2024002");
        Student savedStudent = studentRepository.save(student);

        // When
        Optional<Student> found = studentRepository.findById(savedStudent.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals("João Santos", found.get().getName());
        assertEquals("joao@email.com", found.get().getEmail());
    }

    @Test
    @Order(3)
    @DisplayName("Deve verificar se email existe")
    void testExistsByEmail() {
        // Given
        Student student = new Student("Pedro Costa", "pedro.costa@email.com", "2024003");
        studentRepository.save(student);

        // When
        boolean exists = studentRepository.existsByEmail("pedro.costa@email.com");

        // Then
        assertTrue(exists);
    }

    @Test
    @Order(4)
    @DisplayName("Deve retornar false para email inexistente")
    void testNotExistsByEmail() {
        // When
        boolean exists = studentRepository.existsByEmail("nao.existe@email.com");

        // Then
        assertFalse(exists);
    }

    @Test
    @Order(5)
    @DisplayName("Deve listar todos os estudantes")
    void testFindAllStudents() {
        // Given
        studentRepository.save(new Student("Aluno 1", "aluno1@email.com", "2024004"));
        studentRepository.save(new Student("Aluno 2", "aluno2@email.com", "2024005"));
        studentRepository.save(new Student("Aluno 3", "aluno3@email.com", "2024006"));

        // When
        List<Student> students = studentRepository.findAll();

        // Then
        assertEquals(3, students.size());
    }

    @Test
    @Order(6)
    @DisplayName("Deve deletar estudante por ID")
    void testDeleteStudent() {
        // Given
        Student student = new Student("Ana Lima", "ana.lima@email.com", "2024007");
        Student savedStudent = studentRepository.save(student);
        String studentId = savedStudent.getId();

        // When
        studentRepository.deleteById(studentId);

        // Then
        Optional<Student> deleted = studentRepository.findById(studentId);
        assertFalse(deleted.isPresent());
    }

    @Test
    @Order(7)
    @DisplayName("Deve verificar se matrícula existe")
    void testExistsByRegistrationNumber() {
        // Given
        Student student = new Student("Carlos Souza", "carlos.souza@email.com", "2024008");
        studentRepository.save(student);

        // When
        boolean exists = studentRepository.existsByRegistrationNumber("2024008");

        // Then
        assertTrue(exists);
    }

    @Test
    @Order(8)
    @DisplayName("Deve atualizar dados do estudante")
    void testUpdateStudent() {
        // Given
        Student student = new Student("Nome Antigo", "antigo@email.com", "2024009");
        Student savedStudent = studentRepository.save(student);

        // When
        savedStudent.setName("Nome Novo");
        savedStudent.setEmail("novo@email.com");
        Student updatedStudent = studentRepository.save(savedStudent);

        // Then
        assertEquals("Nome Novo", updatedStudent.getName());
        assertEquals("novo@email.com", updatedStudent.getEmail());
    }
}
