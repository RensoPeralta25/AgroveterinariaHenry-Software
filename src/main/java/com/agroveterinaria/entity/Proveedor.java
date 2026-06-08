package com.agroveterinaria.entity;

import com.agroveterinaria.enums.StatusEntidad;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "proveedor")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Long idProveedor;

    @Column(name = "rnc", nullable = false, length = 20, unique = true)
    @NotBlank(message = "RNC es obligatorio")
    @Pattern(regexp = "^(\\d{9}|\\d{11})$", message = "El RNC debe contener 9 dígitos  para Empresas u 11 dígitos para Personas Físicas")
    private String rnc;

    @Column(name = "nombre", nullable = false, length = 120)
    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras")
    private String nombre;

    @Column(name = "direccion", nullable = false, length = 255)
    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @Column(name = "telefono", nullable = false, length = 20)
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^\\d{3}-\\d{3}-\\d{4}$", message = "El formato debe ser 000-000-0000")
    @Pattern(regexp = "^(809|829|849).*$", message = "El teléfono no es República Dominicana (use 809, 829 u 849)")
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "El status es obligatorio")
    private StatusEntidad status;

    @Column(name = "num_persona_contacto", nullable = false, length = 20)
    @NotBlank(message = "El número de contacto es obligatorio")
    @Pattern(regexp = "^\\d{3}-\\d{3}-\\d{4}$", message = "El formato debe ser 000-000-0000")
    @Pattern(regexp = "^(809|829|849).*$", message = "El número no es República Dominicana (use 809, 829 u 849)")
    private String numPersonaContacto;

    public Proveedor() {
    }

    public Proveedor(Long idProveedor, String rnc, String nombre, String direccion, String telefono,
            StatusEntidad status, String numPersonaContacto) {
        this.idProveedor = idProveedor;
        this.rnc = rnc;
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.status = status;
        this.numPersonaContacto = numPersonaContacto;
    }

    public Long getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Long idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getRnc() {
        return rnc;
    }

    public void setRnc(String rnc) {
        this.rnc = rnc;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public StatusEntidad getStatus() {
        return status;
    }

    public void setStatus(StatusEntidad status) {
        this.status = status;
    }

    public String getNumPersonaContacto() {
        return numPersonaContacto;
    }

    public void setNumPersonaContacto(String numPersonaContacto) {
        this.numPersonaContacto = numPersonaContacto;
    }
}
