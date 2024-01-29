package com.symbol.barcodesample1;

public enum ValidationStatus {
    PENDING,
    WRONG_BUILDING,
    WRONG_EMPLOYEE,
    OK;

    @Override
    public String toString() {
        switch (this) {
            case PENDING:
                return "Lectura Pendiente";
            case WRONG_BUILDING:
                return "Edificio Incorrecto";
            case WRONG_EMPLOYEE:
                return "Empleado Incorrecto";
            case OK:
                return "OK";
            default:
                return "Unknown";
        }
    }
}
