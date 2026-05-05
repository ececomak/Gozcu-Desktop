package com.gozcu.util;

import com.gozcu.model.Operator;

public class SessionManager {
    private static Operator currentOperator;

    public static void login(Operator operator) {
        currentOperator = operator;
    }

    public static void logout() {
        currentOperator = null;
    }

    public static Operator getCurrentOperator() {
        return currentOperator;
    }

    public static boolean isLoggedIn() {
        return currentOperator != null;
    }

    public static boolean isAdmin() {
        return currentOperator != null && "Admin".equalsIgnoreCase(currentOperator.getRole());
    }
}
