package com.example.application;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EventValidatorTest {

    @Test
    public void validateEventName_validation() {
        assertNotNull("Nome nulo deve falhar", EventValidator.validateName(null));
        assertNotNull("Nome com 1 caractere deve falhar", EventValidator.validateName("A"));
        assertNull("Nome com 2 ou mais caracteres deve passar", EventValidator.validateName("Feira Tech"));
    }

    @Test
    public void validateCategory_validation() {
        assertNotNull("Categoria nula deve falhar", EventValidator.validateCategory(null));
        assertNotNull("Categoria vazia deve falhar", EventValidator.validateCategory("   "));
        assertNull("Categoria informada deve passar", EventValidator.validateCategory("Tecnologia"));
    }

    @Test
    public void validateDescription_validation() {
        assertNotNull("Descrição nula deve falhar", EventValidator.validateDescription(null));
        assertNotNull("Descrição curta deve falhar", EventValidator.validateDescription("Curta"));
        assertNull("Descrição com 10 ou mais caracteres deve passar", EventValidator.validateDescription("Descrição completa do evento."));
    }

    @Test
    public void validateLocation_validation() {
        assertNotNull("Local nulo deve falhar", EventValidator.validateLocation(null));
        assertNotNull("Local curto deve falhar", EventValidator.validateLocation("A"));
        assertNull("Local válido deve passar", EventValidator.validateLocation("Auditório Principal"));
    }

    @Test
    public void validateSlots_validation() {
        assertNotNull("Vagas zero deve falhar", EventValidator.validateSlots(0));
        assertNotNull("Vagas negativas deve falhar", EventValidator.validateSlots(-5));
        assertNull("Vagas maiores que zero deve passar", EventValidator.validateSlots(50));
    }

    @Test
    public void isValidEvent_fullValidation() {
        boolean valid = EventValidator.isValidEvent(
                "Hackathon 2026",
                "Tecnologia",
                "Grande evento de programação universitário.",
                "Centro de Convenções",
                100,
                true,
                true
        );
        assertTrue("Evento completo deve ser válido", valid);

        boolean invalidSlots = EventValidator.isValidEvent(
                "Hackathon 2026",
                "Tecnologia",
                "Grande evento de programação universitário.",
                "Centro de Convenções",
                0,
                true,
                true
        );
        assertFalse("Evento com 0 vagas deve ser inválido", invalidSlots);

        boolean invalidDate = EventValidator.isValidEvent(
                "Hackathon 2026",
                "Tecnologia",
                "Grande evento de programação universitário.",
                "Centro de Convenções",
                100,
                false,
                true
        );
        assertFalse("Evento sem data selecionada deve ser inválido", invalidDate);
    }
}
