package com.example.application;

public class EventValidator {

    public static String validateName(String name) {
        if (name == null || name.trim().length() < 2) {
            return "Informe um nome válido com pelo menos 2 caracteres.";
        }
        return null;
    }

    public static String validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "Selecione uma categoria.";
        }
        return null;
    }

    public static String validateDescription(String description) {
        if (description == null || description.trim().length() < 10) {
            return "A descrição deve ter pelo menos 10 caracteres.";
        }
        return null;
    }

    public static String validateLocation(String location) {
        if (location == null || location.trim().length() < 2) {
            return "Informe o local do evento.";
        }
        return null;
    }

    public static String validateSlots(long availableSlots) {
        if (availableSlots <= 0) {
            return "A quantidade de vagas deve ser maior que zero.";
        }
        return null;
    }

    public static boolean isValidEvent(String name, String category, String description,
                                      String location, long slots, boolean isDateSelected, boolean isTimeSelected) {
        return validateName(name) == null &&
                validateCategory(category) == null &&
                validateDescription(description) == null &&
                validateLocation(location) == null &&
                validateSlots(slots) == null &&
                isDateSelected && isTimeSelected;
    }
}
