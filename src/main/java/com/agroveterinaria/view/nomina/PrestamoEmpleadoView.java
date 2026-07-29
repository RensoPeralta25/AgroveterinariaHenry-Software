package com.agroveterinaria.view.nomina;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.dto.nomina.CuotaAmortizacionDTO;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.PrestamoEmpleado;
import com.agroveterinaria.enums.EstadoPrestamo;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.service.EmpleadoService;
import com.agroveterinaria.service.PrestamoEmpleadoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class PrestamoEmpleadoView extends VerticalLayout {
    private final PrestamoEmpleadoService prestamoService;
    private final EmpleadoService empleadoService;
    private final Grid<PrestamoEmpleado> gridPrestamos;
    private final GridPaginator<PrestamoEmpleado> paginator;
    private final TextField txtFiltroEmpleado;
    private final ComboBox<EstadoPrestamo> cmbFiltroEstado;

    public PrestamoEmpleadoView(PrestamoEmpleadoService prestamoService, EmpleadoService empleadoService) {
        this.prestamoService = prestamoService;
        this.empleadoService = empleadoService;
        this.gridPrestamos = new Grid<>(PrestamoEmpleado.class, false);
        this.paginator = new GridPaginator<>(gridPrestamos, 10, "préstamos");

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        Button btnNuevoPrestamo = new Button("Nuevo Préstamo", new Icon(VaadinIcon.PLUS));
        btnNuevoPrestamo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevoPrestamo.addClickListener(e -> dialogFormularioPrestamo(null));

        txtFiltroEmpleado = new TextField();
        txtFiltroEmpleado.setPlaceholder("Buscar por empleado...");
        txtFiltroEmpleado.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        txtFiltroEmpleado.setClearButtonVisible(true);
        txtFiltroEmpleado.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);
        txtFiltroEmpleado.addValueChangeListener(e -> updateList());

        cmbFiltroEstado = new ComboBox<>();
        cmbFiltroEstado.setPlaceholder("Todos los estados");
        cmbFiltroEstado.setItems(EstadoPrestamo.values());
        cmbFiltroEstado.setItemLabelGenerator(EstadoPrestamo::getDescripcion);
        cmbFiltroEstado.setClearButtonVisible(true);
        cmbFiltroEstado.addValueChangeListener(e -> updateList());

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevoPrestamo, txtFiltroEmpleado, cmbFiltroEstado);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.BASELINE);
        toolbar.setFlexGrow(1, txtFiltroEmpleado);

        configurarGrid();

        add(toolbar, paginator, gridPrestamos);
        updateList();
    }

    private void configurarGrid() {
        gridPrestamos.addColumn(p -> p.getEmpleado().getPersona().getNombre() + " " + p.getEmpleado().getPersona().getApellido())
                .setHeader("Empleado").setSortable(true).setFlexGrow(1);

        gridPrestamos.addColumn(PrestamoEmpleado::getConcepto)
                .setHeader("Concepto").setWidth("140px").setFlexGrow(0);

        gridPrestamos.addColumn(p -> "RD$ " + formatearMonto(p.getMontoCapital()))
                .setHeader("Capital Original").setWidth("140px").setFlexGrow(0);

        gridPrestamos.addColumn(p -> "RD$ " + formatearMonto(p.getBalanceCapitalPendiente()))
                .setHeader("Balance Pendiente").setWidth("140px").setFlexGrow(0);

        gridPrestamos.addColumn(p -> "RD$ " + formatearMonto(p.getCuotaPeriodica()))
                .setHeader("Cuota Mensual").setWidth("130px").setFlexGrow(0);

        gridPrestamos.addComponentColumn(prestamo -> {
            Span circulo = new Span();
            circulo.getStyle().set("width", "10px");
            circulo.getStyle().set("height", "10px");
            circulo.getStyle().set("border-radius", "50%");
            circulo.getStyle().set("display", "inline-block");

            Span texto = new Span(prestamo.getEstado().getDescripcion());
            texto.getStyle().set("font-weight", "500");

            String colorBase;
            switch (prestamo.getEstado()) {
                case PENDIENTE -> colorBase = "#e65100";
                case APROBADO -> colorBase = "#2e7d32";
                case SALDADO -> colorBase = "#0d47a1";
                default -> colorBase = "#9e9e9e";
            }

            circulo.getStyle().set("background-color", colorBase);
            texto.getStyle().set("color", colorBase);

            HorizontalLayout layout = new HorizontalLayout(circulo, texto);
            layout.setAlignItems(Alignment.CENTER);
            layout.setSpacing(true);

            return layout;
        }).setHeader("Estado").setWidth("130px").setFlexGrow(0);

        gridPrestamos.addComponentColumn(prestamo -> {
            HorizontalLayout acciones = new HorizontalLayout();
            acciones.setSpacing(false);
            acciones.setPadding(false);

            Button btnVer = new Button(new Icon(VaadinIcon.TABLE));
            btnVer.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnVer.setTooltipText("Ver Amortización");
            btnVer.addClickListener(e -> dialogAmortizacion(prestamo));
            acciones.add(btnVer);

            if (prestamo.getEstado() == EstadoPrestamo.PENDIENTE) {
                Button btnAprobar = new Button(new Icon(VaadinIcon.CHECK));
                btnAprobar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                btnAprobar.setTooltipText("Aprobar");
                btnAprobar.addClickListener(e -> confirmarAprobacion(prestamo));

                Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
                btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                btnEditar.setTooltipText("Editar");
                btnEditar.addClickListener(e -> dialogFormularioPrestamo(prestamo));

                Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
                btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                btnEliminar.setTooltipText("Eliminar");
                btnEliminar.addClickListener(e -> confirmarEliminacion(prestamo));

                acciones.add(btnAprobar, btnEditar, btnEliminar);
            }
            return acciones;
        }).setHeader("Acciones").setWidth("170px").setFlexGrow(0);

        gridPrestamos.addClassName("prestamo-grid");
        gridPrestamos.addThemeNames("row-stripes");
        gridPrestamos.setWidthFull();
        gridPrestamos.setHeight("390px");
    }

    private void dialogFormularioPrestamo(PrestamoEmpleado prestamoExistente) {
        Dialog dialog = new Dialog();
        boolean esNuevo = (prestamoExistente == null);
        dialog.setHeaderTitle(esNuevo ? "Registrar Nuevo Préstamo" : "Editar Préstamo PENDIENTE");
        dialog.setWidth("500px");

        ComboBox<Empleado> cmbEmpleado = new ComboBox<>("Empleado");
        cmbEmpleado.setItems(empleadoService.findByStatus(StatusEntidad.ACTIVO));
        cmbEmpleado.setItemLabelGenerator(e -> e.getPersona().getNombre() + " " + e.getPersona().getApellido());
        cmbEmpleado.setWidthFull();

        TextField conceptoField = new TextField("Concepto");
        conceptoField.setWidthFull();

        BigDecimalField montoCapitalField = crearCampoMoneda("Monto del Capital (RD$)");

        IntegerField plazoMesesField = new IntegerField("Plazo (Meses)");
        plazoMesesField.setWidthFull();
        plazoMesesField.setMin(1);

        BigDecimalField tasaInteresField = new BigDecimalField("Tasa de Interés Anual (%)");
        tasaInteresField.setWidthFull();
        tasaInteresField.setClearButtonVisible(true);
        tasaInteresField.setPlaceholder("Ej. 18.00");

        TextField cuotaPreviewField = new TextField("Cuota Mensual Proyectada");
        cuotaPreviewField.setReadOnly(true);
        cuotaPreviewField.addThemeName("success");

        Runnable actualizarPrevisualizacion = () -> {
            Empleado empleado = cmbEmpleado.getValue();
            BigDecimal capital = montoCapitalField.getValue();
            BigDecimal tasa = tasaInteresField.getValue();
            Integer meses = plazoMesesField.getValue();

            if (empleado != null) {
                try {
                    var limites = prestamoService.calcularLimitesParaUI(empleado);
                    BigDecimal maxCuota = limites.get("maxCuota");
                    int maxMesesPermitidos = limites.get("maxMeses").intValue();
                    BigDecimal minMonto = limites.get("minMonto");
                    BigDecimal maxMonto = limites.get("maxMonto");
                    BigDecimal maxTasa = limites.get("maxTasa");

                    montoCapitalField.setHelperText("Permitido: RD$ " + formatearMonto(minMonto) + " - RD$ " + formatearMonto(maxMonto));

                    tasaInteresField.setHelperText("Límite: " + maxTasa.setScale(2, RoundingMode.HALF_UP) + "% anual");
                    if (tasa != null && tasa.compareTo(maxTasa) > 0) {
                        tasaInteresField.setErrorMessage("Excede el la tasa límite");
                        tasaInteresField.setInvalid(true);
                    } else {
                        tasaInteresField.setInvalid(false);
                    }

                    if (capital != null && capital.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal tasaAUsar = (tasa != null) ? tasa : BigDecimal.ZERO;
                        int minMeses = 1;

                        if (tasaAUsar.compareTo(BigDecimal.ZERO) == 0) {
                            minMeses = capital.divide(maxCuota, 0, RoundingMode.CEILING).intValue();
                        } else {
                            double c = capital.doubleValue();
                            double i = tasaAUsar.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)
                                    .divide(new BigDecimal("12"), 8, RoundingMode.HALF_UP).doubleValue();
                            double m = maxCuota.doubleValue();

                            if (c * i >= m) {
                                minMeses = -1;
                            } else {
                                minMeses = (int) Math.ceil(Math.log(m / (m - c * i)) / Math.log(1 + i));
                            }
                        }

                        if (minMeses == -1) {
                            plazoMesesField.setHelperText("Inválido: Intereses superan el límite salarial de 1/6.");
                            plazoMesesField.setMin(1);
                        } else if (minMeses > maxMesesPermitidos) {
                            plazoMesesField.setHelperText("Requiere min. " + minMeses + " meses (límite global es " + maxMesesPermitidos + "). Reduzca el monto.");
                            plazoMesesField.setMin(minMeses);
                        } else {
                            plazoMesesField.setHelperText("Permitido legalmente: " + minMeses + " a " + maxMesesPermitidos + " meses");
                            plazoMesesField.setMin(minMeses);
                        }
                    }
                } catch (Exception ignored) { }
            } else {
                montoCapitalField.setHelperText("");
                plazoMesesField.setHelperText("Ingrese empleado y monto para calcular límites");
                tasaInteresField.setHelperText("");
            }

            if (capital != null && meses != null && capital.compareTo(BigDecimal.ZERO) > 0 && meses > 0 && !tasaInteresField.isInvalid()) {
                BigDecimal tasaAUsar = (tasa != null) ? tasa : BigDecimal.ZERO;
                BigDecimal cuota = prestamoService.calcularCuotaFrancesa(capital, tasaAUsar, meses);
                cuotaPreviewField.setValue("RD$ " + formatearMonto(cuota));
            } else {
                cuotaPreviewField.clear();
            }
        };

        cmbEmpleado.addValueChangeListener(e -> actualizarPrevisualizacion.run());
        montoCapitalField.addValueChangeListener(e -> actualizarPrevisualizacion.run());
        tasaInteresField.addValueChangeListener(e -> actualizarPrevisualizacion.run());
        plazoMesesField.addValueChangeListener(e -> actualizarPrevisualizacion.run());

        Button btnGuardar = new Button("Guardar", new Icon(VaadinIcon.CHECK), e -> {
            if (cmbEmpleado.isEmpty() || montoCapitalField.isEmpty() || plazoMesesField.isEmpty() || conceptoField.isEmpty()) {
                mostrarError("Todos los campos obligatorios deben estar llenos.");
                return;
            }

            PrestamoEmpleado prestamoAGuardar = esNuevo ? new PrestamoEmpleado() : prestamoExistente;
            prestamoAGuardar.setEmpleado(cmbEmpleado.getValue());
            prestamoAGuardar.setConcepto(conceptoField.getValue());
            prestamoAGuardar.setMontoCapital(montoCapitalField.getValue());
            prestamoAGuardar.setPlazoMeses(plazoMesesField.getValue());
            prestamoAGuardar.setTasaInteres(tasaInteresField.getValue() != null ? tasaInteresField.getValue() : BigDecimal.ZERO);

            try {
                prestamoService.save(prestamoAGuardar);
                mostrarExito("Préstamo guardado exitosamente.");
                dialog.close();
                updateList();
            } catch (IllegalStateException | IllegalArgumentException ex) {
                mostrarError(ex.getMessage());
            } catch (Exception ex) {
                mostrarError("Error crítico al guardar: " + ex.getMessage());
            }
        });
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        if (!esNuevo) {
            cmbEmpleado.setValue(prestamoExistente.getEmpleado());
            cmbEmpleado.setReadOnly(true);
            conceptoField.setValue(prestamoExistente.getConcepto());
            montoCapitalField.setValue(prestamoExistente.getMontoCapital());
            plazoMesesField.setValue(prestamoExistente.getPlazoMeses());
            tasaInteresField.setValue(prestamoExistente.getTasaInteres());
            actualizarPrevisualizacion.run();
        }

        FormLayout formLayout = new FormLayout(cmbEmpleado, conceptoField, montoCapitalField, tasaInteresField, plazoMesesField, cuotaPreviewField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("300px", 2));
        formLayout.setColspan(cmbEmpleado, 2);
        formLayout.setColspan(conceptoField, 2);
        formLayout.setColspan(cuotaPreviewField, 2);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addClassName("btn-borde");

        dialog.add(formLayout);
        dialog.getFooter().add(btnGuardar, btnCancelar);
        dialog.open();
    }

    private void dialogAmortizacion(PrestamoEmpleado prestamo) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Amortización Proyectada - " + prestamo.getConcepto());
        dialog.setWidth("800px");

        Grid<CuotaAmortizacionDTO> gridAmortizacion = new Grid<>(CuotaAmortizacionDTO.class, false);

        gridAmortizacion.addColumn(CuotaAmortizacionDTO::getNumeroCuota)
                .setHeader("Mes").setWidth("70px").setFlexGrow(0);

        gridAmortizacion.addColumn(c -> "RD$ " + formatearMonto(c.getPagoInteres()))
                .setHeader("Interés").setAutoWidth(true);

        gridAmortizacion.addColumn(c -> "RD$ " + formatearMonto(c.getPagoCapital()))
                .setHeader("Abono a Capital").setAutoWidth(true);

        gridAmortizacion.addColumn(c -> "RD$ " + formatearMonto(c.getCuotaTotal()))
                .setHeader("Cuota Total").setAutoWidth(true);

        gridAmortizacion.addColumn(c -> "RD$ " + formatearMonto(c.getBalanceRestante()))
                .setHeader("Balance Restante").setAutoWidth(true);

        gridAmortizacion.addThemeNames("row-stripes");
        gridAmortizacion.setHeight("400px");
        gridAmortizacion.setClassName("amortizacion-grid");


        List<CuotaAmortizacionDTO> proyeccion = prestamoService.generarCuadroAmortizacion(
                prestamo.getBalanceCapitalPendiente(),
                prestamo.getTasaInteres(),
                prestamo.getCuotaPeriodica()
        );

        gridAmortizacion.setItems(proyeccion);

        Button btnCerrar = new Button("Cerrar", e -> dialog.close());
        dialog.getFooter().add(btnCerrar);

        dialog.add(gridAmortizacion);
        dialog.open();
    }

    private void confirmarAprobacion(PrestamoEmpleado prestamo) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Aprobar Préstamo");
        confirm.add(new Span("¿Estás seguro de indicar este préstamo como aprobado? Una vez ejecutado, el motor de nómina comenzará a efectuar los descuentos."));

        Button btnSi = new Button("Sí, Aprobar", e -> {
            try {
                prestamoService.aprobar(prestamo);
                mostrarExito("Préstamo aprobado correctamente.");
                confirm.close();
                updateList();
            } catch (Exception ex) {
                mostrarError(ex.getMessage());
            }
        });
        btnSi.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnNo = new Button("Cancelar", e -> confirm.close());
        btnNo.addClassName("btn-borde");
        confirm.getFooter().add(btnSi, btnNo);
        confirm.open();
    }

    private void confirmarEliminacion(PrestamoEmpleado prestamo) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Eliminar Préstamo");
        confirm.add(new Span("¿Seguro que deseas eliminar este préstamo? Esta acción limpiará el registro físico y no se puede deshacer."));

        Button btnSi = new Button("Sí, Eliminar", e -> {
            try {
                prestamoService.delete(prestamo);
                mostrarExito("Préstamo eliminado con éxito.");
                confirm.close();
                updateList();
            } catch (Exception ex) {
                mostrarError(ex.getMessage());
            }
        });
        btnSi.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button btnNo = new Button("Cancelar", e -> confirm.close());
        btnNo.addClassName("btn-borde");
        confirm.getFooter().add(btnSi, btnNo);
        confirm.open();
    }

    public void updateList() {
        List<PrestamoEmpleado> todosLosPrestamos = prestamoService.findAll();

        String filtroTexto = txtFiltroEmpleado.getValue() != null ? txtFiltroEmpleado.getValue().toLowerCase() : "";
        EstadoPrestamo filtroEstado = cmbFiltroEstado.getValue();

        List<PrestamoEmpleado> filtrados = todosLosPrestamos.stream()
                .filter(prestamo -> {
                    if (filtroTexto.isEmpty()) return true;
                    return prestamo.getEmpleado().getPersona().getNombre().toLowerCase().contains(filtroTexto);
                })
                .filter(prestamo -> {
                    if (filtroEstado == null) return true;
                    return prestamo.getEstado() == filtroEstado;
                })
                .sorted(Comparator.comparing(PrestamoEmpleado::getIdPrestamo).reversed())
                .toList();

        paginator.setItems(filtrados);
    }

    private BigDecimalField crearCampoMoneda(String label) {
        BigDecimalField campo = new BigDecimalField(label);
        campo.setPrefixComponent(new Span("RD$"));
        campo.setWidthFull();
        campo.setClearButtonVisible(true);
        campo.setPlaceholder("0.00");
        return campo;
    }

    private String formatearMonto(BigDecimal monto) {
        if (monto == null) return "0.00";
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "DO"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(monto);
    }

    private void mostrarError(String mensaje) {
        Notification notification = Notification.show(mensaje, 4000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void mostrarExito(String mensaje) {
        Notification notification = Notification.show(mensaje, 3000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}