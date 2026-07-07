package com.agroveterinaria.view.transporte;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.entity.Vehiculo;
import com.agroveterinaria.enums.EstadoVehiculo;
import com.agroveterinaria.service.VehiculoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@PageTitle("Gestión de Vehículos")
@RolesAllowed({"ADMINISTRADOR", "GERENTE_LOGISTICA"})
public class VehiculoView extends VerticalLayout {

    private final VehiculoService vehiculoService;
    private final Grid<Vehiculo> grid = new Grid<>(Vehiculo.class, false);
    private final GridPaginator<Vehiculo> paginator = new GridPaginator<>(grid, 10, "vehículos");

    public VehiculoView(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Button btnNuevo = new Button("Registrar Vehículo", new Icon(VaadinIcon.PLUS));
        btnNuevo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevo.addClickListener(e -> abrirDialogo(new Vehiculo()));

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        configurarGrid();

        add(toolbar, paginator, grid);
        actualizarGrid();
    }

    private void configurarGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
        grid.setHeight("390px");
        grid.addClassName("vehiculo-grid");

        grid.addColumn(Vehiculo::getPlaca).setHeader("Placa").setSortable(true).setFlexGrow(1);
        grid.addColumn(Vehiculo::getMarca).setHeader("Marca").setSortable(true).setFlexGrow(1);
        grid.addColumn(Vehiculo::getModelo).setHeader("Modelo").setFlexGrow(1);
        grid.addColumn(v -> v.getCapacidadCargaKg() + " Kg").setHeader("Capacidad").setFlexGrow(0).setWidth("120px");

        grid.addComponentColumn(v -> {
            Span badge = new Span(v.getEstado().getEtiqueta());
            badge.getElement().getThemeList().add("badge");
            switch (v.getEstado()) {
                case DISPONIBLE -> badge.getElement().getThemeList().add("success");
                case EN_TRANSITO -> badge.getElement().getThemeList().add("contrast");
                case EN_MANTENIMIENTO -> badge.getElement().getThemeList().add("warning");
                case FUERA_DE_SERVICIO -> badge.getElement().getThemeList().add("error");
            }
            return badge;
        }).setHeader("Estado").setFlexGrow(1);

        grid.addComponentColumn(v -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.EDIT));
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.addClickListener(e -> abrirDialogo(v));
            return btnEditar;
        }).setHeader("Acciones").setFlexGrow(0).setWidth("100px");
    }

    private void abrirDialogo(Vehiculo vehiculo) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");

        H3 titulo = new H3(vehiculo.getIdVehiculo() == null ? "Nuevo Vehículo" : "Editar Vehículo");

        TextField txtPlaca = new TextField("Placa");
        TextField txtMarca = new TextField("Marca");
        TextField txtModelo = new TextField("Modelo");
        IntegerField txtAnio = new IntegerField("Año");
        BigDecimalField txtCapacidad = new BigDecimalField("Capacidad (Kg)");

        ComboBox<String> cbCombustible = new ComboBox<>("Combustible");
        cbCombustible.setItems("Diésel", "Gasolina", "GLP", "GNV");

        DatePicker dpSeguro = new DatePicker("Vencimiento Seguro");
        DatePicker dpMatricula = new DatePicker("Vencimiento Matrícula");

        ComboBox<EstadoVehiculo> cbEstado = new ComboBox<>("Estado");
        cbEstado.setItems(EstadoVehiculo.values());
        cbEstado.setItemLabelGenerator(EstadoVehiculo::getEtiqueta);

        if (vehiculo.getIdVehiculo() != null) {
            txtPlaca.setValue(vehiculo.getPlaca());
            txtMarca.setValue(vehiculo.getMarca());
            txtModelo.setValue(vehiculo.getModelo());
            txtAnio.setValue(vehiculo.getAnioFabricacion());
            txtCapacidad.setValue(vehiculo.getCapacidadCargaKg());
            cbCombustible.setValue(vehiculo.getTipoCombustible());
            dpSeguro.setValue(vehiculo.getFechaVencimientoSeguro());
            dpMatricula.setValue(vehiculo.getFechaVencimientoMatricula());
            cbEstado.setValue(vehiculo.getEstado());
        } else {
            cbEstado.setValue(EstadoVehiculo.DISPONIBLE);
        }

        HorizontalLayout fila1 = new HorizontalLayout(txtPlaca, cbEstado);
        fila1.setWidthFull(); fila1.setFlexGrow(1, txtPlaca, cbEstado);

        HorizontalLayout fila2 = new HorizontalLayout(txtMarca, txtModelo, txtAnio);
        fila2.setWidthFull(); fila2.setFlexGrow(1, txtMarca, txtModelo, txtAnio);

        HorizontalLayout fila3 = new HorizontalLayout(txtCapacidad, cbCombustible);
        fila3.setWidthFull(); fila3.setFlexGrow(1, txtCapacidad, cbCombustible);

        HorizontalLayout fila4 = new HorizontalLayout(dpSeguro, dpMatricula);
        fila4.setWidthFull(); fila4.setFlexGrow(1, dpSeguro, dpMatricula);

        VerticalLayout form = new VerticalLayout(fila1, fila2, fila3, fila4);
        form.setPadding(false);

        Button btnGuardar = new Button("Guardar", e -> {
            try {
                vehiculo.setPlaca(txtPlaca.getValue());
                vehiculo.setMarca(txtMarca.getValue());
                vehiculo.setModelo(txtModelo.getValue());
                vehiculo.setAnioFabricacion(txtAnio.getValue());
                vehiculo.setCapacidadCargaKg(txtCapacidad.getValue());
                vehiculo.setTipoCombustible(cbCombustible.getValue());
                vehiculo.setFechaVencimientoSeguro(dpSeguro.getValue());
                vehiculo.setFechaVencimientoMatricula(dpMatricula.getValue());
                vehiculo.setEstado(cbEstado.getValue());

                vehiculoService.guardar(vehiculo);
                actualizarGrid();
                dialog.close();
                Notification.show("Vehículo guardado correctamente").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error: Verifica los campos requeridos y que la placa no se repita.")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout acciones = new HorizontalLayout(btnCancelar, btnGuardar);
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        acciones.setWidthFull();
        acciones.getStyle().set("margin-top", "20px");

        dialog.add(titulo, form, acciones);
        dialog.open();
    }

    private void actualizarGrid() {
        paginator.setItems(vehiculoService.listarTodos());
    }
}
