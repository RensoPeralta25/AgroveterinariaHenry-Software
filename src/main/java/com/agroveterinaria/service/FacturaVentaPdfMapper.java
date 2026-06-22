package com.agroveterinaria.service;

import com.agroveterinaria.dto.venta.FacturaVentaPdfDTO;
import com.agroveterinaria.dto.venta.LineaFacturaVentaPdfDTO;
import com.agroveterinaria.entity.Cliente;
import com.agroveterinaria.entity.DetalleVenta;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.entity.Venta;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FacturaVentaPdfMapper {

    public FacturaVentaPdfDTO toDto(Venta venta, BigDecimal montoCobrado, BigDecimal balancePendiente) {
        Cliente cliente = venta.getCliente();
        Persona personaCliente = cliente != null ? cliente.getPersona() : null;
        Empleado vendedor = venta.getVendedor();
        Persona personaVendedor = vendedor != null ? vendedor.getPersona() : null;

        List<LineaFacturaVentaPdfDTO> lineas = venta.getDetallesVentas().stream()
                .map(this::toLineaDto)
                .toList();

        return new FacturaVentaPdfDTO(
                venta.getIdVenta(),
                venta.getFechaHoraVenta(),
                valorOrDefault(personaCliente != null ? personaCliente.getNombre() : null, "Cliente sin nombre"),
                valorOrDefault(personaCliente != null ? personaCliente.getCedula() : null, "Sin cedula"),
                valorOrDefault(personaCliente != null ? personaCliente.getTelefono() : null, "Sin telefono"),
                valorOrDefault(personaCliente != null ? personaCliente.getDireccion() : null, "Sin direccion"),
                valorOrDefault(personaVendedor != null ? personaVendedor.getNombre() : null, "Sin vendedor"),
                valorOrDefault(venta.getComprobanteFiscal(), "Sin comprobante"),
                venta.getEstado() != null ? venta.getEstado().getEtiqueta() : "",
                Boolean.TRUE.equals(venta.getLlevaDespacho()),
                venta.calcularSubtotalDetalles(),
                montoSeguro(venta.getMontoTotal()),
                montoSeguro(montoCobrado),
                montoSeguro(balancePendiente),
                lineas
        );
    }

    private LineaFacturaVentaPdfDTO toLineaDto(DetalleVenta detalle) {
        Producto producto = detalle.getProducto();
        return new LineaFacturaVentaPdfDTO(
                valorOrDefault(producto != null ? producto.getNombre() : null, "Producto sin nombre"),
                montoSeguro(detalle.getCantidad()),
                montoSeguro(detalle.getPrecioUnitarioVenta()),
                montoSeguro(detalle.getImpuesto()),
                detalle.calcularSubtotal()
        );
    }

    private BigDecimal montoSeguro(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String valorOrDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}
