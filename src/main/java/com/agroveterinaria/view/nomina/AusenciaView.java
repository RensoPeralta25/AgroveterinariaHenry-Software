package com.agroveterinaria.view.nomina;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.entity.Ausencia;
import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.enums.EstadoRegistro;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.enums.TipoAusencia;
import com.agroveterinaria.service.AusenciaService;
import com.agroveterinaria.service.ConfiguracionNominaService;
import com.agroveterinaria.service.EmpleadoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class AusenciaView extends VerticalLayout {
    private final AusenciaService ausenciaService;
    private final EmpleadoService empleadoService;
    private final ConfiguracionNominaService configuracionNominaService;

    private final Grid<Ausencia> gridAusencias;
    private final GridPaginator<Ausencia> paginator;
    private final TextField txtFiltroEmpleado;
    private final ComboBox<TipoAusencia> cmbFiltroTipo;
    ComboBox<EstadoRegistro> cmbFiltroEstado = new ComboBox<>();

    public AusenciaView(AusenciaService ausenciaService, EmpleadoService empleadoService, ConfiguracionNominaService configuracionNominaService) {
        this.ausenciaService = ausenciaService;
        this.empleadoService = empleadoService;
        this.configuracionNominaService = configuracionNominaService;
        this.gridAusencias = new Grid<>(Ausencia.class, false);
        this.paginator = new GridPaginator<>(gridAusencias, 10, "ausencias");

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        Button btnNuevaAusencia = new Button("Registrar Ausencia", new Icon(VaadinIcon.PLUS));
        btnNuevaAusencia.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevaAusencia.addClickListener(e -> dialogFormularioAusencia(null));

        txtFiltroEmpleado = new TextField();
        txtFiltroEmpleado.setPlaceholder("Buscar por empleado...");
        txtFiltroEmpleado.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        txtFiltroEmpleado.setClearButtonVisible(true);
        txtFiltroEmpleado.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);
        txtFiltroEmpleado.addValueChangeListener(e -> updateList());

        cmbFiltroTipo = new ComboBox<>();
        cmbFiltroTipo.setPlaceholder("Todos los tipos");
        cmbFiltroTipo.setItems(TipoAusencia.values());
        cmbFiltroTipo.setItemLabelGenerator(TipoAusencia::getDescripcion);
        cmbFiltroTipo.setClearButtonVisible(true);
        cmbFiltroTipo.addValueChangeListener(e -> updateList());

        cmbFiltroEstado.setPlaceholder("Todos los estados");
        cmbFiltroEstado.setItems(EstadoRegistro.values());
        cmbFiltroEstado.setItemLabelGenerator(EstadoRegistro::getDescripcion);
        cmbFiltroEstado.setClearButtonVisible(true);
        cmbFiltroEstado.addValueChangeListener(e -> updateList());

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevaAusencia, txtFiltroEmpleado, cmbFiltroTipo, cmbFiltroEstado);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.BASELINE);
        toolbar.setFlexGrow(1, txtFiltroEmpleado);

        configurarGrid();

        add(toolbar, paginator, gridAusencias);
        updateList();
    }

    private void configurarGrid() {
        gridAusencias.addColumn(a -> a.getEmpleado().getPersona().getNombre() + " " + a.getEmpleado().getPersona().getApellido())
                .setHeader("Empleado").setSortable(true).setFlexGrow(1);

        gridAusencias.addColumn(a -> a.getTipoAusencia().getDescripcion())
                .setHeader("Tipo").setWidth("180px").setFlexGrow(0);

        gridAusencias.addColumn(Ausencia::getFechaInicio)
                .setHeader("Desde").setWidth("120px").setFlexGrow(0);

        gridAusencias.addColumn(a -> a.getFechaFin() != null ? a.getFechaFin().toString() : "Indefinida")
                .setHeader("Hasta").setWidth("110px").setFlexGrow(0);

        gridAusencias.addColumn(a -> {
            if (a.getEstadoRegistro() == EstadoRegistro.ABIERTA) {
                return ausenciaService.calcularDiasAusenciaEnRango(a, a.getFechaInicio(), LocalDate.now()) + " (A hoy)";
            }
            return String.valueOf(ausenciaService.calcularDiasAusenciaEnRango(a, a.getFechaInicio(), a.getFechaFin()));
        }).setHeader("Días").setWidth("110px").setFlexGrow(0);

        gridAusencias.addComponentColumn(ausencia -> {
            String colorBase;
            String textoJustificacion;

            if (ausencia.getDocumentoAdjunto() != null) {
                colorBase = "#2e7d32";
                textoJustificacion = "Soporte Adjunto";
            } else {
                long plazoHoras = configuracionNominaService.getPlazoJustificacionHoras();
                long diasDePlazo = plazoHoras / 24;

                LocalDate fechaInicio = ausencia.getFechaInicio();
                LocalDate hoy = LocalDate.now();
                int diasHabilesPasados = 0;

                if (fechaInicio != null && hoy.isAfter(fechaInicio)) {
                    LocalDate fechaIterador = fechaInicio.plusDays(1);
                    while (!fechaIterador.isAfter(hoy)) {
                        if (fechaIterador.getDayOfWeek() != DayOfWeek.SUNDAY) {
                            diasHabilesPasados++;
                        }
                        fechaIterador = fechaIterador.plusDays(1);
                    }
                }

                if (diasHabilesPasados > diasDePlazo) {
                    colorBase = "#dc2626";
                    textoJustificacion = "Plazo Vencido";
                } else {
                    colorBase = "#f59e0b";
                    textoJustificacion = "Pendiente";
                }
            }

            Span circulo = new Span();
            circulo.getStyle().set("width", "10px").set("height", "10px")
                    .set("border-radius", "50%").set("display", "inline-block")
                    .set("background-color", colorBase);

            Span texto = new Span(textoJustificacion);
            texto.getStyle().set("font-weight", "500").set("color", colorBase);

            HorizontalLayout layout = new HorizontalLayout(circulo, texto);
            layout.setAlignItems(Alignment.CENTER);
            layout.setSpacing(true);

            return layout;
        }).setHeader("Justificación").setWidth("160px").setFlexGrow(0);

        gridAusencias.addComponentColumn(ausencia -> {
            String colorBase = ausencia.isAplicadaEnNomina() ? "#2e7d32" : "#f59e0b";
            String textoEstado = ausencia.isAplicadaEnNomina() ? "Aplicada" : "Pendiente";

            Span circulo = new Span();
            circulo.getStyle().set("width", "10px").set("height", "10px")
                    .set("border-radius", "50%").set("display", "inline-block")
                    .set("background-color", colorBase);

            Span texto = new Span(textoEstado);
            texto.getStyle().set("font-weight", "500").set("color", colorBase);

            HorizontalLayout layout = new HorizontalLayout(circulo, texto);
            layout.setAlignItems(Alignment.CENTER);
            layout.setSpacing(true);
            return layout;
        }).setHeader("Estado").setWidth("120px").setFlexGrow(0);

        gridAusencias.addComponentColumn(ausencia -> {
            HorizontalLayout acciones = new HorizontalLayout();
            acciones.setSpacing(false);
            acciones.setPadding(false);

            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.setTooltipText("Editar / Justificar");
            btnEditar.addClickListener(e -> dialogFormularioAusencia(ausencia));
            acciones.add(btnEditar);

            if (ausencia.getDocumentoAdjunto() != null) {
                Anchor downloadLink = new Anchor(
                        new StreamResource(ausencia.getNombreArchivo(), () -> new ByteArrayInputStream(ausencia.getDocumentoAdjunto())),
                        ""
                );
                downloadLink.getElement().setAttribute("download", true);
                Button btnDescargar = new Button(new Icon(VaadinIcon.DOWNLOAD));
                btnDescargar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                btnDescargar.setTooltipText("Descargar justificación");
                downloadLink.add(btnDescargar);
                acciones.add(downloadLink);
            }

            if (!ausencia.isAplicadaEnNomina()) {
                Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
                btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                btnEliminar.setTooltipText("Eliminar");
                btnEliminar.addClickListener(e -> confirmarEliminacion(ausencia));
                acciones.add(btnEliminar);
            }

            return acciones;
        }).setHeader("Acciones").setWidth("150px").setFlexGrow(0);

        gridAusencias.addClassName("ausencia-grid");
        gridAusencias.addThemeNames("row-stripes");
        gridAusencias.setWidthFull();
        gridAusencias.setHeight("390px");
    }

    private void dialogFormularioAusencia(Ausencia ausenciaExistente) {
        Dialog dialog = new Dialog();
        boolean esNuevo = (ausenciaExistente == null);
        dialog.setHeaderTitle(esNuevo ? "Registrar Ausencia" : "Editar Ausencia");
        dialog.setWidth("500px");

        ComboBox<Empleado> cmbEmpleado = new ComboBox<>("Empleado");
        cmbEmpleado.setItems(empleadoService.findByStatus(StatusEntidad.ACTIVO));
        cmbEmpleado.setItemLabelGenerator(e -> e.getPersona().getNombre() + " " + e.getPersona().getApellido());
        cmbEmpleado.setWidthFull();

        ComboBox<TipoAusencia> cmbTipo = new ComboBox<>("Motivo de Ausencia");
        cmbTipo.setItems(TipoAusencia.values());
        cmbTipo.setItemLabelGenerator(TipoAusencia::getDescripcion);
        cmbTipo.setWidthFull();

        ComboBox<EstadoRegistro> cmbEstado = new ComboBox<>("Estado del Registro");
        cmbEstado.setItems(EstadoRegistro.values());
        cmbEstado.setItemLabelGenerator(EstadoRegistro::getDescripcion);
        cmbEstado.setWidthFull();

        DatePicker fechaInicio = new DatePicker("Fecha de Inicio");
        fechaInicio.setWidthFull();

        DatePicker fechaFin = new DatePicker("Fecha de Fin");
        fechaFin.setWidthFull();

        fechaInicio.addValueChangeListener(e -> fechaFin.setMin(e.getValue()));

        MemoryBuffer buffer = new MemoryBuffer();
        Upload uploadJustificacion = new Upload(buffer);
        uploadJustificacion.setAcceptedFileTypes("application/pdf", "image/jpeg", "image/png");
        uploadJustificacion.setMaxFiles(1);
        uploadJustificacion.setDropLabel(new Span("Arrastre documento de justificación (PDF/JPG/PNG)"));
        uploadJustificacion.setWidthFull();

        final byte[][] archivoTemporal = {null};
        final String[] nombreArchivoTemporal = {null};

        uploadJustificacion.addSucceededListener(event -> {
            try {
                archivoTemporal[0] = buffer.getInputStream().readAllBytes();
                nombreArchivoTemporal[0] = event.getFileName();
                mostrarExito("Archivo listo para guardar.");
            } catch (Exception ex) {
                mostrarError("Error al procesar el archivo.");
            }
        });

        cmbEstado.addValueChangeListener(e -> {
            if (e.getValue() == null) return;
            boolean esCerrada = (e.getValue() == EstadoRegistro.CERRADA);
            fechaFin.setRequiredIndicatorVisible(esCerrada);

            if (!esCerrada) {
                fechaFin.clear();
                fechaFin.setReadOnly(true);
            } else {
                fechaFin.setReadOnly(false);
            }
        });

        Button btnGuardar = new Button("Guardar", new Icon(VaadinIcon.CHECK), e -> {
            if (cmbEmpleado.isEmpty() || cmbTipo.isEmpty() || cmbEstado.isEmpty() || fechaInicio.isEmpty()) {
                mostrarError("Los campos obligatorios deben estar llenos.");
                return;
            }

            if (cmbEstado.getValue() == EstadoRegistro.CERRADA && fechaFin.isEmpty()) {
                mostrarError("La fecha de fin es obligatoria si el estado es CERRADO.");
                return;
            }

            if (fechaFin.getValue() != null && fechaInicio.getValue() != null) {
                if (fechaFin.getValue().isBefore(fechaInicio.getValue())) {
                    mostrarError("La fecha de fin no puede ser anterior al inicio.");
                    return;
                }
            }

            boolean tieneAdjuntoNuevo = archivoTemporal[0] != null;
            boolean tieneAdjuntoPrevio = (!esNuevo && ausenciaExistente.getDocumentoAdjunto() != null);

            if ((tieneAdjuntoNuevo || tieneAdjuntoPrevio) && cmbTipo.getValue() == TipoAusencia.INJUSTIFICADA) {
                mostrarError("No puedes clasificar la ausencia como INJUSTIFICADA si estás adjuntando un documento de soporte.");
                return;
            }

            Ausencia ausenciaAGuardar = esNuevo ? new Ausencia() : ausenciaExistente;
            ausenciaAGuardar.setEmpleado(cmbEmpleado.getValue());
            ausenciaAGuardar.setTipoAusencia(cmbTipo.getValue());
            ausenciaAGuardar.setEstadoRegistro(cmbEstado.getValue());
            ausenciaAGuardar.setFechaInicio(fechaInicio.getValue());
            ausenciaAGuardar.setFechaFin(fechaFin.getValue());

            if (tieneAdjuntoNuevo) {
                ausenciaAGuardar.setDocumentoAdjunto(archivoTemporal[0]);
                ausenciaAGuardar.setNombreArchivo(nombreArchivoTemporal[0]);
            }

            if (esNuevo) {
                ausenciaAGuardar.setAplicadaEnNomina(false);
                ausenciaAGuardar.setFechaRegistro(LocalDateTime.now());
            }

            try {
                ausenciaService.registrarAusencia(ausenciaAGuardar);
                mostrarExito("Ausencia guardada exitosamente.");
                dialog.close();
                updateList();
            } catch (Exception ex) {
                mostrarError("Error al guardar: " + ex.getMessage());
            }
        });
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        if (!esNuevo) {
            cmbEmpleado.setValue(ausenciaExistente.getEmpleado());
            cmbTipo.setValue(ausenciaExistente.getTipoAusencia());
            cmbEstado.setValue(ausenciaExistente.getEstadoRegistro());
            fechaInicio.setValue(ausenciaExistente.getFechaInicio());
            if (ausenciaExistente.getFechaFin() != null) {
                fechaFin.setValue(ausenciaExistente.getFechaFin());
            }

            if (ausenciaExistente.isAplicadaEnNomina()) {
                cmbEmpleado.setReadOnly(true);
                fechaInicio.setReadOnly(true);
                fechaFin.setReadOnly(true);
                cmbEstado.setReadOnly(true);
            } else {
                cmbEmpleado.setReadOnly(true);
                fechaInicio.setReadOnly(true);
            }

        } else {
            cmbEstado.setValue(EstadoRegistro.CERRADA);
            fechaInicio.setValue(LocalDate.now());
            fechaFin.setValue(LocalDate.now());
        }

        Div divArchivoActual = new Div();
        divArchivoActual.getStyle().set("margin-bottom", "10px");
        divArchivoActual.getStyle().set("font-size", "var(--lumo-font-size-s)");

        if (!esNuevo && ausenciaExistente.getDocumentoAdjunto() != null) {
            Icon icon = VaadinIcon.FILE_TEXT.create();
            icon.setSize("16px");
            icon.getStyle().set("margin-right", "5px");
            icon.setColor("var(--lumo-primary-text-color)");

            Span txtArchivo = new Span("Archivo actual: " + ausenciaExistente.getNombreArchivo());
            txtArchivo.getStyle().set("color", "var(--lumo-primary-text-color)");

            divArchivoActual.add(icon, txtArchivo);
        }

        FormLayout formLayout = new FormLayout(cmbEmpleado, cmbTipo, cmbEstado,fechaInicio, fechaFin);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("300px", 2));
        formLayout.setColspan(cmbEmpleado, 2);

        VerticalLayout layoutConUpload = new VerticalLayout(formLayout, divArchivoActual,uploadJustificacion);
        layoutConUpload.setPadding(false);
        layoutConUpload.setSpacing(true);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addClassName("btn-borde");

        dialog.add(layoutConUpload);
        dialog.getFooter().add(btnGuardar, btnCancelar);
        dialog.open();
    }

    private void confirmarEliminacion(Ausencia ausencia) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Eliminar Ausencia");
        confirm.add(new Span("¿Seguro que deseas eliminar este registro de ausencia? Esta acción no se puede deshacer."));

        Button btnSi = new Button("Sí, Eliminar", e -> {
            try {
                ausenciaService.delete(ausencia);
                mostrarExito("Registro de ausencia eliminado con éxito.");
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
        List<Ausencia> todasLasAusencias = ausenciaService.findAllConRelaciones();

        String filtroTexto = txtFiltroEmpleado.getValue() != null ? txtFiltroEmpleado.getValue().toLowerCase() : "";
        TipoAusencia filtroTipo = cmbFiltroTipo.getValue();
        EstadoRegistro filtroEstado = cmbFiltroEstado.getValue();

        List<Ausencia> filtrados = todasLasAusencias.stream()
                .filter(ausencia -> {
                    if (filtroTexto.isEmpty()) return true;
                    String nombreCompleto = ausencia.getEmpleado().getPersona().getNombre().toLowerCase() + " " +
                            ausencia.getEmpleado().getPersona().getApellido().toLowerCase();
                    return nombreCompleto.contains(filtroTexto);
                })
                .filter(ausencia -> {
                    if (filtroTipo == null) return true;
                    return ausencia.getTipoAusencia() == filtroTipo;
                })
                .filter(ausencia -> {
                    if (filtroEstado == null) return true;
                    return ausencia.getEstadoRegistro() == filtroEstado;
                })
                .sorted(Comparator.comparing(Ausencia::getId).reversed())
                .toList();

        paginator.setItems(filtrados);
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
