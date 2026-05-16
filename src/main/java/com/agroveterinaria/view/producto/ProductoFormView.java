package com.agroveterinaria.view.producto;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.formlayout.FormLayout;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;

import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;

import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.UnidadMedida;

import java.math.BigDecimal;

@Route("productos/nuevo")
@PageTitle("Nuevo Producto")
public class ProductoFormView extends VerticalLayout {

    private final TextField nombre = new TextField("Nombre del producto");
    private final BigDecimalField precioUnitario = new BigDecimalField("Precio unitario");
    private final ComboBox<CategoriaProducto> categoria = new ComboBox<>("Categoría");
    private final BigDecimalField presentacion = new BigDecimalField("Presentación");
    private final ComboBox<UnidadMedida> unidadMedida = new ComboBox<>("Unidad de medida");

    private final Button btnGuardar  = new Button("Guardar");
    private final Button btnCancelar = new Button("Cancelar");

    private final Binder<Producto> binder = new Binder<>(Producto.class);

    public ProductoFormView() {
        configurarCampos();
        configurarBinder();
        configurarBotones();
        construirLayout();
    }


    private void configurarCampos() {

        nombre.setPlaceholder("Ej: Amoxicilina 500mg");
        nombre.setWidth("100%");

        precioUnitario.setPlaceholder("0.00");
        precioUnitario.setPrefixComponent(new com.vaadin.flow.component.html.Span("RD$"));
        precioUnitario.setWidth("100%");

        categoria.setItems(CategoriaProducto.values());
        categoria.setItemLabelGenerator(CategoriaProducto::getEtiqueta);
        categoria.setPlaceholder("Selecciona una categoría");
        categoria.setWidth("100%");

        presentacion.setPlaceholder("Ej: 100, 250, 500");
        presentacion.setWidth("100%");

        unidadMedida.setItems(UnidadMedida.values());
        unidadMedida.setItemLabelGenerator(UnidadMedida::getEtiqueta);
        unidadMedida.setPlaceholder("Selecciona una unidad");
        unidadMedida.setWidth("100%");
    }

    private void configurarBinder() {

        binder.forField(nombre)
                .asRequired("El nombre es obligatorio")
                .withValidator(n -> n.length() >= 2,
                        "El nombre debe tener al menos 2 caracteres")
                .bind(Producto::getNombre, Producto::setNombre);

        binder.forField(precioUnitario)
                .asRequired("El precio es obligatorio")
                .withValidator(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0,
                        "El precio debe ser mayor a 0")
                .bind(Producto::getPrecioUnitario, Producto::setPrecioUnitario);

        binder.forField(categoria)
                .asRequired("La categoría es obligatoria")
                .bind(Producto::getCategoria, Producto::setCategoria);

        binder.forField(presentacion)
                .asRequired("La presentación es obligatoria")
                .withValidator(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0,
                        "La presentación debe ser mayor a 0")
                .bind(Producto::getPresentacion, Producto::setPresentacion);

        binder.forField(unidadMedida)
                .asRequired("La unidad de medida es obligatoria")
                .bind(Producto::getUnidadMedida, Producto::setUnidadMedida);
    }

    private void configurarBotones() {

        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnGuardar.addClickListener(e -> guardar());

        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnCancelar.addClickListener(e -> limpiarFormulario());
    }

    private void guardar() {
        Producto producto = new Producto();

        try {
            binder.writeBean(producto);

            System.out.println("Producto a guardar: " + producto.getNombre()
                    + " | Precio: " + producto.getPrecioUnitario()
                    + " | Categoría: " + producto.getCategoria()
                    + " | Presentación: " + producto.getPresentacion()
                    + " " + producto.getUnidadMedida());

            mostrarNotificacion(
                    "Producto '" + producto.getNombre() + "' creado correctamente",
                    NotificationVariant.LUMO_SUCCESS
            );
            limpiarFormulario();

        } catch (ValidationException ex) {
            mostrarNotificacion(
                    "Corrige los errores antes de guardar",
                    NotificationVariant.LUMO_ERROR
            );
        }
    }

    private void construirLayout() {

        H2 titulo = new H2("Registrar nuevo producto");

        FormLayout formulario = new FormLayout();
        formulario.add(nombre, precioUnitario, categoria, presentacion, unidadMedida);

        formulario.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0px",   1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        formulario.setColspan(nombre, 2);

        HorizontalLayout botones = new HorizontalLayout(btnGuardar, btnCancelar);
        botones.setSpacing(true);

        add(titulo, new Hr(), formulario, botones);

        setWidth("700px");
        setMaxWidth("100%");
        setPadding(true);
        setSpacing(true);
        getStyle().set("margin", "0 auto");
    }


    private void mostrarNotificacion(String mensaje, NotificationVariant variante) {
        Notification notif = Notification.show(mensaje, 3000, Position.TOP_CENTER);
        notif.addThemeVariants(variante);
    }

    private void limpiarFormulario() {
        binder.readBean(new Producto());
    }
}