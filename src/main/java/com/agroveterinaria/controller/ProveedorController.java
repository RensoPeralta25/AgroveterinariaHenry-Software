package com.agroveterinaria.controller;

import com.agroveterinaria.entity.Proveedor;
import com.agroveterinaria.service.ProveedorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController (ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Proveedor> obtenerPorId(@PathVariable Long id) {
        return proveedorService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Proveedor> insertarProveedor(@RequestBody Proveedor proveedor) {
        Proveedor proveedorGuardado = proveedorService.guardar(proveedor);
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorGuardado);
    }

}
