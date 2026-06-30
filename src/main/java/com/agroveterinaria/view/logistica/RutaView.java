package com.agroveterinaria.view.logistica;

import com.agroveterinaria.entity.Ruta;
import com.agroveterinaria.entity.RutaParada;
import com.agroveterinaria.service.RutaService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@PageTitle("Configuración de Rutas")
@RolesAllowed({"ADMINISTRADOR", "ASISTENTE"})
public class RutaView extends VerticalLayout {

    private final RutaService rutaService;
    private final Grid<Ruta> grid = new Grid<>(Ruta.class, false);

    public RutaView(RutaService rutaService) {
        this.rutaService = rutaService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Rutas de Distribución Estándar");
        titulo.getStyle().set("margin-top", "0");

        Button btnNuevo = new Button("Nueva Ruta", new Icon(VaadinIcon.PLUS));
        btnNuevo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevo.addClickListener(e -> abrirDialogo(new Ruta()));

        HorizontalLayout toolbar = new HorizontalLayout(titulo, btnNuevo);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        configurarGrid();

        add(toolbar, grid);
        actualizarGrid();
    }

    private void configurarGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();
        grid.addClassName("ruta-grid");

        grid.addColumn(Ruta::getNombre).setHeader("Nombre de la Ruta").setSortable(true).setFlexGrow(2);

        grid.addColumn(r -> r.getDistanciaKm() != null ? r.getDistanciaKm() + " Km" : "-")
                .setHeader("Distancia").setFlexGrow(1);

        grid.addColumn(r -> {
            if (r.getTiempoEstimado() == null) return "-";
            long horas = r.getTiempoEstimado().toHours();
            long minutos = r.getTiempoEstimado().toMinutesPart();
            return String.format("%dh %dm", horas, minutos);
        }).setHeader("Tiempo Estimado").setFlexGrow(1);

        grid.addColumn(r -> r.getParadas() != null ? r.getParadas().size() + " paradas" : "0 paradas")
                .setHeader("Escalas").setFlexGrow(1);

        grid.addComponentColumn(r -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.EDIT));
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.addClickListener(e -> abrirDialogo(r));

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnEliminar.addClickListener(e -> abrirConfirmacionEliminar(r));

            HorizontalLayout accionesRow = new HorizontalLayout(btnEditar, btnEliminar);
            accionesRow.setSpacing(true);
            return accionesRow;
        }).setHeader("Acciones").setFlexGrow(0).setWidth("140px");
    }

    private void abrirConfirmacionEliminar(Ruta ruta) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("¿Eliminar Ruta Permanentemente?");

        confirmDialog.setMaxWidth("450px");

        com.vaadin.flow.component.html.Paragraph mensaje = new com.vaadin.flow.component.html.Paragraph(
                "¿Está seguro de que desea eliminar la ruta \"" + ruta.getNombre() + "\"? " +
                        "Esta operación no se puede deshacer y fallará si la ruta tiene dependencias en operaciones logísticas."
        );

        VerticalLayout cuerpoLayout = new VerticalLayout(mensaje);
        cuerpoLayout.setPadding(false);
        confirmDialog.add(cuerpoLayout);

        Button btnConfirmar = new Button("Eliminar", ev -> {
            try {
                rutaService.eliminar(ruta.getIdRuta());
                actualizarGrid();
                confirmDialog.close();
                Notification.show("Ruta eliminada correctamente").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalStateException ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                Notification.show("Error inesperado al intentar borrar el registro").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnConfirmar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button btnCancelar = new Button("Cancelar", ev -> confirmDialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        confirmDialog.getFooter().add(btnCancelar, btnConfirmar);
        confirmDialog.open();
    }

    private void abrirDialogo(Ruta ruta) {
        Dialog dialog = new Dialog();
        dialog.setWidth("750px");

        H3 titulo = new H3(ruta.getIdRuta() == null ? "Registrar Nueva Ruta" : "Editar Configuración de Ruta");
        titulo.getStyle().set("margin-top", "0");

        TextField txtNombre = new TextField("Nombre de la Ruta");
        txtNombre.setPlaceholder("Ej. Ruta Norte - Puerto Plata");
        txtNombre.setWidthFull();

        BigDecimalField txtDistancia = new BigDecimalField("Distancia (Km)");
        txtDistancia.setWidthFull();

        IntegerField txtHoras = new IntegerField("Horas");
        txtHoras.setMin(0);
        txtHoras.setWidthFull();

        IntegerField txtMinutos = new IntegerField("Minutos");
        txtMinutos.setMin(0);
        txtMinutos.setMax(59);
        txtMinutos.setWidthFull();

        H4 tituloParadas = new H4("Puntos de Escala (Paradas)");
        tituloParadas.getStyle().set("margin-top", "15px").set("margin-bottom", "5px");

        List<RutaParada> paradasActuales = new ArrayList<>(ruta.getParadas());
        Grid<RutaParada> gridParadas = new Grid<>(RutaParada.class, false);
        gridParadas.addThemeNames("compact", "row-stripes");
        gridParadas.setHeight("180px");

        gridParadas.addColumn(RutaParada::getOrden).setHeader("N°").setWidth("50px").setFlexGrow(0);
        gridParadas.addColumn(RutaParada::getDireccion).setHeader("Dirección / Punto").setFlexGrow(2);
        gridParadas.addColumn(p -> p.getLatitud() != null ? p.getLatitud() : "-").setHeader("Latitud").setWidth("100px").setFlexGrow(0);
        gridParadas.addColumn(p -> p.getLongitud() != null ? p.getLongitud() : "-").setHeader("Longitud").setWidth("100px").setFlexGrow(0);

        gridParadas.addComponentColumn(p -> {
            Button btnQuitar = new Button(new Icon(VaadinIcon.TRASH));
            btnQuitar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnQuitar.addClickListener(ev -> {
                paradasActuales.remove(p);
                reordenarParadas(paradasActuales);
                gridParadas.setItems(paradasActuales);
            });
            return btnQuitar;
        }).setWidth("60px").setFlexGrow(0);

        gridParadas.setItems(paradasActuales);

        List<RutaParada> paradasFrecuentes = extraerParadasFrecuentes();

        ComboBox<RutaParada> cbSugerirParada = new ComboBox<>("Dirección / Punto");
        cbSugerirParada.setPlaceholder("Escriba o seleccione...");
        cbSugerirParada.setItems(paradasFrecuentes);
        cbSugerirParada.setItemLabelGenerator(RutaParada::getDireccion);
        cbSugerirParada.setAllowCustomValue(true);
        cbSugerirParada.setWidthFull();

        NumberField numLatitud = new NumberField("Latitud");
        numLatitud.setPlaceholder("Ej. 19.451");
        numLatitud.setWidth("130px");

        NumberField numLongitud = new NumberField("Longitud");
        numLongitud.setPlaceholder("Ej. -70.692");
        numLongitud.setWidth("130px");

        cbSugerirParada.addValueChangeListener(e -> {
            RutaParada seleccionada = e.getValue();
            if (seleccionada != null) {
                numLatitud.setValue(seleccionada.getLatitud());
                numLongitud.setValue(seleccionada.getLongitud());
            }
        });

        cbSugerirParada.addCustomValueSetListener(e -> {
            String nuevaDireccion = e.getDetail();
            RutaParada temporal = new RutaParada();
            temporal.setDireccion(nuevaDireccion);
            cbSugerirParada.setValue(temporal);
            numLatitud.clear();
            numLongitud.clear();
            numLatitud.focus();
        });

        Button btnAgregarParada = new Button(new Icon(VaadinIcon.PLUS));
        btnAgregarParada.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        btnAgregarParada.getStyle().set("margin-bottom", "3px");

        btnAgregarParada.addClickListener(ev -> {
            if (cbSugerirParada.getValue() != null && cbSugerirParada.getValue().getDireccion() != null) {
                RutaParada nueva = new RutaParada();
                nueva.setDireccion(cbSugerirParada.getValue().getDireccion());
                nueva.setLatitud(numLatitud.getValue());
                nueva.setLongitud(numLongitud.getValue());
                nueva.setOrden(paradasActuales.size() + 1);

                paradasActuales.add(nueva);
                gridParadas.setItems(paradasActuales);

                cbSugerirParada.clear();
                numLatitud.clear();
                numLongitud.clear();
                cbSugerirParada.focus();
            } else {
                Notification.show("Ingrese o seleccione una dirección válida").addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
        });

        HorizontalLayout filaCamposParada = new HorizontalLayout(cbSugerirParada, numLatitud, numLongitud, btnAgregarParada);
        filaCamposParada.setWidthFull();
        filaCamposParada.setAlignItems(FlexComponent.Alignment.END);

        if (ruta.getIdRuta() != null) {
            txtNombre.setValue(ruta.getNombre() != null ? ruta.getNombre() : "");
            txtDistancia.setValue(ruta.getDistanciaKm());

            if (ruta.getTiempoEstimado() != null) {
                txtHoras.setValue((int) ruta.getTiempoEstimado().toHours());
                txtMinutos.setValue(ruta.getTiempoEstimado().toMinutesPart());
            }
        } else {
            txtHoras.setValue(0);
            txtMinutos.setValue(0);
            txtDistancia.setValue(BigDecimal.ZERO);
        }

        HorizontalLayout filaTiempo = new HorizontalLayout(txtHoras, txtMinutos);
        filaTiempo.setWidthFull();

        VerticalLayout form = new VerticalLayout(txtNombre, txtDistancia, filaTiempo, tituloParadas, gridParadas, filaCamposParada);
        form.setPadding(false);

        Button btnGuardar = new Button("Guardar", e -> {
            if (txtNombre.isEmpty() || txtDistancia.getValue() == null) {
                Notification.show("Complete los campos obligatorios").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                ruta.setNombre(txtNombre.getValue());
                ruta.setDistanciaKm(txtDistancia.getValue());

                int h = txtHoras.getValue() != null ? txtHoras.getValue() : 0;
                int m = txtMinutos.getValue() != null ? txtMinutos.getValue() : 0;
                ruta.setTiempoEstimado(Duration.ofHours(h).plusMinutes(m));

                ruta.getParadas().clear();
                for (RutaParada p : paradasActuales) {
                    p.setRuta(ruta);
                    ruta.getParadas().add(p);
                }

                rutaService.guardar(ruta);
                actualizarGrid();
                dialog.close();
                Notification.show("Ruta configurada con éxito").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error al guardar: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout acciones = new HorizontalLayout(btnCancelar, btnGuardar);
        acciones.setWidthFull();
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        acciones.getStyle().set("margin-top", "20px");

        dialog.add(titulo, form, acciones);
        dialog.open();
    }

    private void reordenarParadas(List<RutaParada> lista) {
        for (int i = 0; i < lista.size(); i++) {
            lista.get(i).setOrden(i + 1);
        }
    }

    private void actualizarGrid() {
        grid.setItems(rutaService.listarTodos());
    }

    private List<RutaParada> extraerParadasFrecuentes() {
        List<Ruta> todasLasRutas = rutaService.listarTodos();
        List<RutaParada> listaUnicas = new ArrayList<>();
        List<String> direccionesRegistradas = new ArrayList<>();

        for (Ruta r : todasLasRutas) {
            if (r.getParadas() != null) {
                for (RutaParada p : r.getParadas()) {
                    if (!direccionesRegistradas.contains(p.getDireccion().trim().toUpperCase())) {
                        direccionesRegistradas.add(p.getDireccion().trim().toUpperCase());
                        listaUnicas.add(p);
                    }
                }
            }
        }
        return listaUnicas;
    }
}