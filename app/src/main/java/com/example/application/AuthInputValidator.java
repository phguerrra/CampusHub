package com.example.application;

public class AuthInputValidator {

    public static String validateName(String name) {
        if (name == null || name.trim().length() < 2) {
            return "Informe seu nome.";
        }
        return null;
    }

    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty() || !email.contains("@") || email.endsWith("@") || !email.substring(email.indexOf("@")).contains(".")) {
            return "Informe um e-mail válido.";
        }
        return null;
    }

    public static String validatePassword(String password) {
        if (password == null || password.length() < 6) {
            return "A senha deve ter pelo menos 6 caracteres.";
        }
        return null;
    }
}
