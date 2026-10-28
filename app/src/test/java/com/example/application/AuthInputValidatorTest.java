package com.example.application;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class AuthInputValidatorTest {

    @Test
    public void validateName_validAndInvalidCases() {
        assertNotNull("Nome nulo deve retornar erro", AuthInputValidator.validateName(null));
        assertNotNull("Nome vazio deve retornar erro", AuthInputValidator.validateName("   "));
        assertNotNull("Nome com 1 caractere deve retornar erro", AuthInputValidator.validateName("A"));

        assertNull("Nome com 2 caracteres deve ser válido", AuthInputValidator.validateName("Ana"));
        assertNull("Nome completo deve ser válido", AuthInputValidator.validateName("Pedro Silva"));
    }

    @Test
    public void validateEmail_validAndInvalidCases() {
        assertNotNull("E-mail nulo deve retornar erro", AuthInputValidator.validateEmail(null));
        assertNotNull("E-mail sem @ deve retornar erro", AuthInputValidator.validateEmail("usuarioexemplo.com"));
        assertNotNull("E-mail sem domínio deve retornar erro", AuthInputValidator.validateEmail("usuario@"));

        assertNull("E-mail padrão deve ser válido", AuthInputValidator.validateEmail("aluno@universidade.edu.br"));
        assertNull("E-mail simples deve ser válido", AuthInputValidator.validateEmail("teste@exemplo.com"));
    }

    @Test
    public void validatePassword_validAndInvalidCases() {
        assertNotNull("Senha nula deve retornar erro", AuthInputValidator.validatePassword(null));
        assertNotNull("Senha com 5 caracteres deve retornar erro", AuthInputValidator.validatePassword("12345"));

        assertNull("Senha com 6 caracteres deve ser válida", AuthInputValidator.validatePassword("123456"));
        assertNull("Senha forte deve ser válida", AuthInputValidator.validatePassword("SenhaForte123!"));
    }
}
