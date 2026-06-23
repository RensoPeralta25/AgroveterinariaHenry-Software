package com.agroveterinaria.exception;

/**
 * Excepción con un mensaje apto para mostrar al usuario durante una operación
 * del módulo de proveedores.
 */
public class ProveedorOperacionException extends RuntimeException {

    public ProveedorOperacionException(String message, Throwable cause) {
        super(message, cause);
    }
}
