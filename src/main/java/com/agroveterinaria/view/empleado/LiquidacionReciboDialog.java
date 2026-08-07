package com.agroveterinaria.view.empleado;

import com.agroveterinaria.entity.LiquidacionEmpleado;
import com.agroveterinaria.enums.MotivoSalida;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class LiquidacionReciboDialog extends Dialog {

    private final NumberFormat formatoMoneda;
    private final DateTimeFormatter formatoFecha;

    public LiquidacionReciboDialog(LiquidacionEmpleado liquidacion) {
        this.formatoMoneda = NumberFormat.getNumberInstance(new Locale("es", "DO"));
        this.formatoMoneda.setMinimumFractionDigits(2);
        this.formatoMoneda.setMaximumFractionDigits(2);

        this.formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        setWidth("500px");
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);

        construirUI(liquidacion);
    }

    private void construirUI(LiquidacionEmpleado liquidacion) {
        H3 titulo = new H3("Recibo de Liquidación");
        titulo.getStyle().set("margin-top", "0").set("text-align", "center");

        String nombreCompleto = liquidacion.getEmpleado().getPersona().getNombre() + " " +
                liquidacion.getEmpleado().getPersona().getApellido();

        VerticalLayout cabeceraInfo = new VerticalLayout(
                new Span("Empleado: " + nombreCompleto),
                new Span("Cédula: " + liquidacion.getEmpleado().getPersona().getCedula()),
                new Span("Motivo: " + liquidacion.getMotivoSalida().name()),
                new Span("Fecha: " + liquidacion.getFechaLiquidacion().format(formatoFecha))
        );
        cabeceraInfo.setSpacing(false);
        cabeceraInfo.setPadding(false);

        VerticalLayout ingresosLayout = new VerticalLayout();
        ingresosLayout.setSpacing(false);
        ingresosLayout.setPadding(false);
        ingresosLayout.add(new H4("Ingresos (Derechos Adquiridos y Prestaciones)"));

        if (esMayorACero(liquidacion.getMontoRegalia())) {
            ingresosLayout.add(crearFila("Regalía Pascual Proporcional:", liquidacion.getMontoRegalia(), false));
        }

        if (esMayorACero(liquidacion.getMontoVacaciones())) {
            ingresosLayout.add(crearFila("Vacaciones Proporcionales:", liquidacion.getMontoVacaciones(), false));
        }

        if (esMayorACero(liquidacion.getMontoPreaviso()) && liquidacion.getMotivoSalida() == MotivoSalida.DESAHUCIO) {
            ingresosLayout.add(crearFila("Preaviso:", liquidacion.getMontoPreaviso(), false));
        }

        if (esMayorACero(liquidacion.getMontoCesantia())) {
            ingresosLayout.add(crearFila("Cesantía:", liquidacion.getMontoCesantia(), false));
        }
        ingresosLayout.add(new Hr());
        ingresosLayout.add(crearFila("Total Ingresos:", liquidacion.getTotalIngresos(), true));

        VerticalLayout deduccionesLayout = new VerticalLayout();
        deduccionesLayout.setSpacing(false);
        deduccionesLayout.setPadding(false);

        if (esMayorACero(liquidacion.getTotalDeducciones())) {
            deduccionesLayout.add(new H4("Deducciones (Deudas y Retenciones)"));

            if (esMayorACero(liquidacion.getDescuentoEmbargos())) {
                deduccionesLayout.add(crearFila("Retención Embargos (Pensión):", liquidacion.getDescuentoEmbargos(), false));
            }

            if (esMayorACero(liquidacion.getDescuentoAnticipos())) {
                deduccionesLayout.add(crearFila("Cobro Anticipos Pendientes:", liquidacion.getDescuentoAnticipos(), false));
            }

            if (esMayorACero(liquidacion.getDescuentoPrestamos())) {
                deduccionesLayout.add(crearFila("Cobro Préstamos Pendientes:", liquidacion.getDescuentoPrestamos(), false));
            }

            if (esMayorACero(liquidacion.getMontoPreaviso()) && liquidacion.getMotivoSalida() == MotivoSalida.RENUNCIA) {
                deduccionesLayout.add(crearFila("Preaviso:", liquidacion.getMontoPreaviso(), false));
            }
            deduccionesLayout.add(new Hr());
            deduccionesLayout.add(crearFila("Total Deducciones:", liquidacion.getTotalDeducciones(), true));
        }

        VerticalLayout netoLayout = new VerticalLayout();
        netoLayout.setPadding(false);

        HorizontalLayout filaNeto = crearFila("TOTAL NETO A PAGAR:", liquidacion.getMontoNeto(), true);
        filaNeto.getStyle().set("font-size", "1.2em").set("color", "var(--lumo-primary-text-color)");
        netoLayout.add(new Hr(), filaNeto, new Hr());

        VerticalLayout mainLayout = new VerticalLayout(titulo, cabeceraInfo, ingresosLayout, deduccionesLayout, netoLayout);
        mainLayout.setPadding(false);
        add(mainLayout);

        Button btnCerrar = new Button("Cerrar", e -> close());

        getFooter().add(btnCerrar);
    }

    private HorizontalLayout crearFila(String concepto, BigDecimal monto, boolean destacado) {
        Span lblConcepto = new Span(concepto);
        Span lblMonto = new Span("RD$ " + formatoMoneda.format(monto));

        if (destacado) {
            lblConcepto.getStyle().set("font-weight", "bold");
            lblMonto.getStyle().set("font-weight", "bold");
        }

        HorizontalLayout fila = new HorizontalLayout(lblConcepto, lblMonto);
        fila.setWidthFull();
        fila.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return fila;
    }

    private boolean esMayorACero(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }
}