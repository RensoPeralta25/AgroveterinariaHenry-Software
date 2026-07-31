package com.agroveterinaria.view.nomina;

import com.agroveterinaria.component.DatosTransferenciaForm;
import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.entity.AbonoAnticipo;
import com.agroveterinaria.entity.AnticipoSalario;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.enums.EstadoAnticipo;
import com.agroveterinaria.enums.EstadoPrestamo;
import com.agroveterinaria.enums.MetodoPago;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.repository.EmbargoSalarialRepository;
import com.agroveterinaria.repository.PrestamoEmpleadoRepository;
import com.agroveterinaria.security.SecurityService;
import com.agroveterinaria.service.AnticipoSalarioService;
import com.agroveterinaria.service.CuentaBancariaTransferenciaPdfService;
import com.agroveterinaria.service.EmpleadoService;
import com.agroveterinaria.service.PrestamoEmpleadoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class AnticipoSalarioView extends VerticalLayout {
    private final AnticipoSalarioService anticipoService;
    private final EmpleadoService empleadoService;
    private final CuentaBancariaTransferenciaPdfService cuentaBancariaTransferenciaPdfService;
    private final SecurityService securityService;

    private final Grid<AnticipoSalario> gridAnticipos;
    private final GridPaginator<AnticipoSalario> paginator;
    private final TextField txtFiltroEmpleado;
    private final ComboBox<EstadoAnticipo> cmbFiltroEstado;

    public AnticipoSalarioView(AnticipoSalarioService anticipoService, EmpleadoService empleadoService,
                               CuentaBancariaTransferenciaPdfService cuentaBancariaTransferenciaPdfService, SecurityService securityService) {
        this.anticipoService = anticipoService;
        this.empleadoService = empleadoService;
        this.cuentaBancariaTransferenciaPdfService = cuentaBancariaTransferenciaPdfService;
        this.securityService = securityService;
        this.gridAnticipos = new Grid<>(AnticipoSalario.class, false);
        this.paginator = new GridPaginator<>(gridAnticipos, 10, "anticipos");

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        Button btnNuevoAnticipo = new Button("Nuevo Anticipo", new Icon(VaadinIcon.PLUS));
        btnNuevoAnticipo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevoAnticipo.addClickListener(e -> dialogFormularioAnticipo(null));

        txtFiltroEmpleado = new com.vaadin.flow.component.textfield.TextField();
        txtFiltroEmpleado.setPlaceholder("Buscar por empleado...");
        txtFiltroEmpleado.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        txtFiltroEmpleado.setClearButtonVisible(true);

        txtFiltroEmpleado.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);
        txtFiltroEmpleado.addValueChangeListener(e -> updateList());

        cmbFiltroEstado = new ComboBox<>();
        cmbFiltroEstado.setPlaceholder("Todos los estados");
        cmbFiltroEstado.setItems(EstadoAnticipo.values());
        cmbFiltroEstado.setItemLabelGenerator(EstadoAnticipo::getDescripcion);
        cmbFiltroEstado.setClearButtonVisible(true);
        cmbFiltroEstado.addValueChangeListener(e -> updateList());

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevoAnticipo, txtFiltroEmpleado, cmbFiltroEstado);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.BASELINE);
        toolbar.setFlexGrow(1, txtFiltroEmpleado);

        configurarGrid();

        add(toolbar, paginator, gridAnticipos);
        updateList();
    }

    private void configurarGrid() {
        gridAnticipos.addColumn(a -> a.getEmpleado().getPersona().getNombre() + " " + a.getEmpleado().getPersona().getApellido())
                .setHeader("Empleado").setSortable(true).setFlexGrow(1);

        gridAnticipos.addColumn(AnticipoSalario::getFechaRegistro)
                .setHeader("Fecha de Emisión").setWidth("140px").setFlexGrow(0);

        gridAnticipos.addColumn(a -> "RD$ " + formatearMonto(a.getMontoOriginal()))
                .setHeader("Monto Original").setWidth("140px").setFlexGrow(0);

        gridAnticipos.addColumn(a -> "RD$ " + formatearMonto(a.getMontoDescontado()))
                .setHeader("Descontado").setWidth("130px").setFlexGrow(0);

        gridAnticipos.addColumn(a -> "RD$ " + formatearMonto(a.getSaldoPendiente()))
                .setHeader("Saldo Pendiente").setWidth("140px").setFlexGrow(0);

        gridAnticipos.addColumn(a -> "RD$ " + formatearMonto(a.getCuotaDescuento()))
                .setHeader("Cuota/Ciclo").setWidth("130px").setFlexGrow(0);

        gridAnticipos.addComponentColumn(anticipo -> {
            Span circulo = new Span();
            circulo.getStyle().set("width", "10px");
            circulo.getStyle().set("height", "10px");
            circulo.getStyle().set("border-radius", "50%");
            circulo.getStyle().set("display", "inline-block");

            Span texto = new Span(anticipo.getEstado().getDescripcion());
            texto.getStyle().set("font-weight", "500");

            String colorBase;
            switch (anticipo.getEstado()) {
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

        gridAnticipos.addComponentColumn(anticipo -> {
            HorizontalLayout acciones = new HorizontalLayout();
            acciones.setSpacing(false);
            acciones.setPadding(false);

            if (anticipo.getEstado() == EstadoAnticipo.PENDIENTE) {
                Button btnAprobar = new Button(new Icon(VaadinIcon.CHECK));
                btnAprobar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                btnAprobar.setTooltipText("Aprobar");
                btnAprobar.addClickListener(e -> confirmarAprobacion(anticipo));

                Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
                btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                btnEditar.setTooltipText("Editar");
                btnEditar.addClickListener(e -> dialogFormularioAnticipo(anticipo));

                Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
                btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                btnEliminar.setTooltipText("Eliminar");
                btnEliminar.addClickListener(e -> confirmarEliminacion(anticipo));

                acciones.add(btnAprobar, btnEditar, btnEliminar);
            } else if (anticipo.getEstado() == EstadoAnticipo.APROBADO || anticipo.getEstado() == EstadoAnticipo.SALDADO) {

                if (anticipo.getEstado() == EstadoAnticipo.APROBADO) {
                    Button btnAbonar = new Button(new Icon(VaadinIcon.DOLLAR));
                    btnAbonar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                    btnAbonar.setTooltipText("Registrar Abono al Anticipo");
                    btnAbonar.addClickListener(e -> dialogAbonoExtraordinario(anticipo));
                    acciones.add(btnAbonar);
                }
                Button btnHistorial = new Button(new Icon(VaadinIcon.CLOCK));
                btnHistorial.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                btnHistorial.setTooltipText("Ver Historial de Abonos");
                btnHistorial.addClickListener(e -> dialogHistorialAbonos(anticipo));
                acciones.add(btnHistorial);
            }


            return acciones;
        }).setHeader("Acciones").setWidth("150px").setFlexGrow(0);

        gridAnticipos.addClassName("usuario-grid");
        gridAnticipos.addThemeNames("row-stripes");
        gridAnticipos.setWidthFull();
        gridAnticipos.setHeight("390px");
    }

    private void dialogFormularioAnticipo(AnticipoSalario anticipoExistente) {
        Dialog dialog = new Dialog();
        boolean esNuevo = (anticipoExistente == null);
        dialog.setHeaderTitle(esNuevo ? "Registrar Nuevo Anticipo" : "Editar Anticipo PENDIENTE");
        dialog.setWidth("450px");

        ComboBox<Empleado> cmbEmpleado = new ComboBox<>("Empleado");
        cmbEmpleado.setItems(empleadoService.findByStatus(StatusEntidad.ACTIVO));
        cmbEmpleado.setItemLabelGenerator(e -> e.getPersona().getNombre() + " " + e.getPersona().getApellido());
        cmbEmpleado.setWidthFull();

        DatePicker fechaField = new DatePicker("Fecha de Emisión");
        fechaField.setWidthFull();

        LocalDate hoy = LocalDate.now();
        fechaField.setMin(hoy.withDayOfMonth(1));
        fechaField.setMax(hoy.withDayOfMonth(hoy.lengthOfMonth()));

        BigDecimalField montoOriginalField = crearCampoMoneda("Monto Total Autorizado (RD$)");
        BigDecimalField cuotaField = crearCampoMoneda("Cuota a Descontar por Nómina (RD$)");

        Button btnGuardar = new Button("Guardar", new Icon(VaadinIcon.CHECK), e -> {
            if (cmbEmpleado.isEmpty() || fechaField.isEmpty() || montoOriginalField.isEmpty() || cuotaField.isEmpty()) {
                mostrarError("Todos los campos son obligatorios.");
                return;
            }

            BigDecimal monto = montoOriginalField.getValue();
            BigDecimal cuota = cuotaField.getValue();

            if (monto.compareTo(BigDecimal.ZERO) <= 0 || cuota.compareTo(BigDecimal.ZERO) <= 0) {
                mostrarError("Los montos deben ser mayores a cero.");
                return;
            }
            if (cuota.compareTo(monto) > 0) {
                mostrarError("La cuota no puede ser mayor al monto total.");
                return;
            }

            AnticipoSalario anticipoAGuardar = esNuevo ? new AnticipoSalario() : anticipoExistente;
            anticipoAGuardar.setEmpleado(cmbEmpleado.getValue());
            anticipoAGuardar.setFechaRegistro(fechaField.getValue());
            anticipoAGuardar.setMontoOriginal(monto);
            anticipoAGuardar.setCuotaDescuento(cuota);

            try {
                anticipoService.save(anticipoAGuardar);
                mostrarExito("Anticipo guardado exitosamente.");
                dialog.close();
                updateList();
            } catch (IllegalStateException | IllegalArgumentException ex) {
                mostrarError(ex.getMessage());
            } catch (Exception ex) {
                mostrarError("Error crítico al guardar: " + ex.getMessage());
            }
        });
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        cmbEmpleado.addValueChangeListener(event -> {
            Empleado empleadoSeleccionado = event.getValue();

            if (empleadoSeleccionado != null) {
                var limites = anticipoService.calcularLimitesParaUI(empleadoSeleccionado);

                montoOriginalField.setHelperText("Permitido: RD$ " + formatearMonto(limites.get("minMonto")) +
                        " - RD$ " + formatearMonto(limites.get("maxMonto")));

                BigDecimal montoActual = montoOriginalField.getValue();

                if (montoActual != null && montoActual.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal maxCuotaTeorica = limites.get("maxCuota");
                    BigDecimal plazoMaximo = limites.get("plazoMaximo");

                    BigDecimal cuotaMinima = montoActual.divide(plazoMaximo, 2, RoundingMode.HALF_UP);
                    BigDecimal cuotaMaxima = maxCuotaTeorica.min(montoActual);

                    cuotaField.setHelperText("Permitido: RD$ " + formatearMonto(cuotaMinima) +
                            " - RD$ " + formatearMonto(cuotaMaxima));
                } else {
                    cuotaField.setHelperText("Ingrese un monto para ver los límites permitidos");
                }
            } else {
                montoOriginalField.setHelperText("");
                cuotaField.setHelperText("");
            }
        });

        montoOriginalField.addValueChangeListener(event -> {
            Empleado empleadoSeleccionado = cmbEmpleado.getValue();
            BigDecimal montoActual = event.getValue();

            if (empleadoSeleccionado != null) {
                var limites = anticipoService.calcularLimitesParaUI(empleadoSeleccionado);
                BigDecimal maxCuotaTeorica = limites.get("maxCuota");
                BigDecimal plazoMaximo = limites.get("plazoMaximo");

                if (montoActual != null && montoActual.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal cuotaMinima = montoActual.divide(plazoMaximo, 2, RoundingMode.HALF_UP);
                    BigDecimal cuotaMaxima = maxCuotaTeorica.min(montoActual);

                    cuotaField.setHelperText("Permitido: RD$ " + formatearMonto(cuotaMinima) +
                            " - RD$ " + formatearMonto(cuotaMaxima));
                } else {
                    cuotaField.setHelperText("Ingrese un monto para ver los límites permitidos");
                }
            }
        });

        if (!esNuevo) {
            cmbEmpleado.setValue(anticipoExistente.getEmpleado());
            cmbEmpleado.setReadOnly(true);
            fechaField.setValue(anticipoExistente.getFechaRegistro());
            montoOriginalField.setValue(anticipoExistente.getMontoOriginal());
            cuotaField.setValue(anticipoExistente.getCuotaDescuento());
        } else {
            fechaField.setValue(hoy);
        }

        FormLayout formLayout = new FormLayout(cmbEmpleado, fechaField, montoOriginalField, cuotaField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addClassName("btn-borde");

        dialog.add(formLayout);
        dialog.getFooter().add(btnGuardar, btnCancelar);
        dialog.open();
    }

    private void dialogAbonoExtraordinario(AnticipoSalario anticipo) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Registrar Abono a Anticipo");
        dialog.setWidth("500px");

        Span lblInfo = new Span("Saldo Pendiente actual: RD$ " + formatearMonto(anticipo.getSaldoPendiente()));
        lblInfo.getStyle().set("font-weight", "bold").set("color", "var(--lumo-primary-text-color)");

        BigDecimalField txtMonto = crearCampoMoneda("Monto a Abonar");
        txtMonto.setAutofocus(true);

        ComboBox<MetodoPago> cmbMetodo = new ComboBox<>("Método de Pago");
        cmbMetodo.setItems(MetodoPago.EFECTIVO, MetodoPago.TRANSFERENCIA);
        cmbMetodo.setItemLabelGenerator(MetodoPago::getEtiqueta);
        cmbMetodo.setWidthFull();

        DatosTransferenciaForm datosTransferencia = new DatosTransferenciaForm();
        datosTransferencia.setVisible(false);

        Anchor btnDescargarCuenta = crearDescargaCuentaBancaria();
        btnDescargarCuenta.setVisible(false);

        cmbMetodo.addValueChangeListener(e -> {
            boolean esTransferencia = e.getValue() == MetodoPago.TRANSFERENCIA;
            datosTransferencia.setVisible(esTransferencia);
            btnDescargarCuenta.setVisible(esTransferencia);

            if (!esTransferencia) {
                datosTransferencia.limpiar();
            } else {
                datosTransferencia.sugerirTitular(anticipo.getEmpleado().getPersona().getNombre() + " " +anticipo.getEmpleado().getPersona().getApellido());
            }
        });

        Button btnGuardar = new Button("Aplicar Abono", new Icon(VaadinIcon.CHECK), e -> {
            if (txtMonto.isEmpty() || cmbMetodo.isEmpty()) {
                mostrarError("Complete todos los campos base para registrar el abono.");
                return;
            }

            AbonoAnticipo abono = new AbonoAnticipo();
            abono.setAnticipoSalario(anticipo);
            abono.setMonto(txtMonto.getValue());
            abono.setMetodoPago(cmbMetodo.getValue());
            abono.setFechaAbono(LocalDate.now());
            abono.setEmpleadoRegistrador(securityService.obtenerEmpleadoAutenticado());

            if (cmbMetodo.getValue() == MetodoPago.TRANSFERENCIA) {
                try {
                    var dt = datosTransferencia.obtenerDatos();
                    if (dt == null) {
                        mostrarError("Debes completar los datos y el comprobante de la transferencia.");
                        return;
                    }

                    if (dt.comprobante() == null || dt.comprobante().length == 0) {
                        mostrarError("Es obligatorio adjuntar la imagen o PDF del comprobante.");
                        return;
                    }

                    if (!dt.confirmadaPorCajero()) {
                        mostrarError("Debes marcar la casilla confirmando que revisaste el comprobante.");
                        return;
                    }

                    abono.setBancoOrigen(dt.bancoOrigen());
                    abono.setTitularTransferencia(dt.titular());
                    abono.setReferenciaTransferencia(dt.referencia());
                    abono.setComprobanteTransferencia(dt.comprobante());
                    abono.setNombreComprobante(dt.nombreComprobante());
                    abono.setTipoContenidoComprobante(dt.tipoContenido());
                    abono.setFechaConfirmacionTransferencia(LocalDateTime.now());

                } catch (Exception ex) {
                    mostrarError(ex.getMessage());
                    return;
                }
            }

            try {
                anticipoService.registrarAbonoExtraordinario(abono);
                mostrarExito("Abono procesado correctamente.");
                dialog.close();
                updateList();
            } catch (Exception ex) {
                mostrarError(ex.getMessage());
            }
        });
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addClassName("btn-borde");

        FormLayout layout = new FormLayout(lblInfo, txtMonto, cmbMetodo, btnDescargarCuenta, datosTransferencia);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        dialog.add(layout);
        dialog.getFooter().add(btnGuardar, btnCancelar);
        dialog.open();
    }

    private void dialogHistorialAbonos(AnticipoSalario anticipo) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Historial de Abonos - Anticipo #" + anticipo.getId());
        dialog.setWidth("800px");

        Grid<AbonoAnticipo> gridAbonos = new Grid<>(AbonoAnticipo.class, false);
        gridAbonos.addThemeNames("row-stripes");
        gridAbonos.setHeight("350px");

        gridAbonos.addColumn(abono -> abono.getFechaAbono().toString())
                .setHeader("Fecha")
                .setWidth("110px").setFlexGrow(0);

        gridAbonos.addColumn(abono -> "RD$ " + formatearMonto(abono.getMonto()))
                .setHeader("Monto")
                .setWidth("140px").setFlexGrow(0);

        gridAbonos.addColumn(abono -> abono.getMetodoPago().getEtiqueta())
                .setHeader("Método")
                .setWidth("130px").setFlexGrow(0);

        gridAbonos.addColumn(abono -> {
            if (abono.getEmpleadoRegistrador() != null && abono.getEmpleadoRegistrador().getPersona() != null) {
                return abono.getEmpleadoRegistrador().getPersona().getNombre() + " " +
                        abono.getEmpleadoRegistrador().getPersona().getApellido();
            }
            return "Sistema";
        }).setHeader("Registrado Por").setWidth("160px").setFlexGrow(0);

        gridAbonos.addColumn(abono -> abono.getReferenciaTransferencia() != null ? abono.getReferenciaTransferencia() : "-")
                .setHeader("Referencia")
                .setFlexGrow(1);

        gridAbonos.addComponentColumn(this::crearDescargaComprobanteAbono)
                .setHeader("Comprobante")
                .setWidth("120px").setFlexGrow(0);

        gridAbonos.setItems(anticipoService.obtenerHistorialAbonos(anticipo.getId()));
        gridAbonos.addClassName("abono-grid");

        Button btnCerrar = new Button("Cerrar", e -> dialog.close());
        btnCerrar.addClassName("btn-borde");

        dialog.getFooter().add(btnCerrar);
        dialog.add(gridAbonos);
        dialog.open();
    }

    private Component crearDescargaComprobanteAbono(AbonoAnticipo abono) {
        byte[] contenido = abono.getComprobanteTransferencia();
        if (contenido == null || contenido.length == 0) {
            return new Span("-");
        }

        String nombre = abono.getNombreComprobante() != null ? abono.getNombreComprobante() : "comprobante-abono";
        String tipo = abono.getTipoContenidoComprobante() != null ? abono.getTipoContenidoComprobante() : "application/octet-stream";

        StreamResource resource = new StreamResource(nombre, () -> new ByteArrayInputStream(contenido));
        resource.setContentType(tipo);
        resource.setCacheTime(0);

        Anchor descarga = new Anchor(resource, "Descargar");
        descarga.getElement().setAttribute("download", true);
        return descarga;
    }

    private Anchor crearDescargaCuentaBancaria() {
        StreamResource resource = new StreamResource("cuenta-bancaria-transferencia.pdf", () ->
                new ByteArrayInputStream(cuentaBancariaTransferenciaPdfService.generarCuentaBancariaPdf()));
        resource.setContentType("application/pdf");
        resource.setCacheTime(0);

        Button descargar = new Button("Ver Cuenta Bancaria", new Icon(VaadinIcon.MONEY));
        descargar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        descargar.setWidthFull();
        descargar.setTooltipText("Descargar PDF con los datos bancarios de la empresa");

        Anchor anchor = new Anchor(resource, "");
        anchor.getElement().setAttribute("download", true);
        anchor.setWidthFull();
        anchor.add(descargar);
        return anchor;
    }

    private void confirmarAprobacion(AnticipoSalario anticipo) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Aprobar Anticipo");
        confirm.add(new Span("¿Estás seguro de indicar este anticipo como aprobado? Una vez ejecutado, se bloqueará su edición o borrado, quedando disponible para el motor de nómina."));

        Button btnSi = new Button("Sí, Aprobar", e -> {
            try {
                anticipoService.aprobar(anticipo);
                mostrarExito("Anticipo aprobado correctamente.");
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

    private void confirmarEliminacion(AnticipoSalario anticipo) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Eliminar Anticipo");
        confirm.add(new Span("¿Seguro que deseas eliminar este anticipo? Esta acción limpiará el registro físico y no se puede deshacer."));

        Button btnSi = new Button("Sí, Eliminar", e -> {
            try {
                anticipoService.delete(anticipo);
                mostrarExito("Anticipo eliminado con éxito.");
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
        List<AnticipoSalario> todosLosAnticipos = anticipoService.findAll();

        String filtroTexto = txtFiltroEmpleado.getValue() != null ? txtFiltroEmpleado.getValue().toLowerCase() : "";
        EstadoAnticipo filtroEstado = cmbFiltroEstado.getValue();

        List<AnticipoSalario> filtrados = todosLosAnticipos.stream()
                .filter(anticipo -> {
                    if (filtroTexto.isEmpty()) return true;
                    String nombreEmpleado = anticipo.getEmpleado().getPersona().getNombre().toLowerCase();
                    return nombreEmpleado.contains(filtroTexto);
                })
                .filter(anticipo -> {
                    if (filtroEstado == null) return true;
                    return anticipo.getEstado() == filtroEstado;
                })
                .sorted(Comparator.comparing(AnticipoSalario::getFechaRegistro).reversed())
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
        Notification notification = Notification.show(mensaje, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void mostrarExito(String mensaje) {
        Notification notification = Notification.show(mensaje, 3000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
