package com.agroveterinaria.view.lote;

import com.agroveterinaria.entity.Lote;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.service.LoteService;
import com.agroveterinaria.service.ProductoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.time.format.DateTimeFormatter;
import java.util.Base64;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
@CssImport(value = "./sorter-styles.css", themeFor = "vaadin-grid-sorter")
@PageTitle("Gestión de Lotes")
@RolesAllowed({"ADMINISTRADOR", "ASISTENTE", "AUDITOR"})
@Route("/lotes")
public class LoteView extends VerticalLayout {

    private final LoteService loteService;
    private final ProductoService productoService;

    public LoteView(LoteService loteService, ProductoService productoService) {
        this.loteService = loteService;
        this.productoService = productoService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        GridCrud<Lote> crudLote = new GridCrud<>(Lote.class, new WindowBasedCrudLayout());
        crudLote.addClassName("lote-crud");
        crudLote.getGrid().addClassName("almacen-grid");

        crudLote.getGrid().removeAllColumns();

        crudLote.getGrid().addColumn(lote -> lote.getNumeroLote() != null ? lote.getNumeroLote() : "S/N")
                .setHeader("N° de Lote")
                .setKey("numeroLote")
                .setWidth("160px").setFlexGrow(0).setSortable(true);

        crudLote.getGrid().addComponentColumn(lote -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(Alignment.CENTER);
            layout.setSpacing(true);

            Image avatar = new Image();
            avatar.setWidth("44px");
            avatar.setHeight("44px");
            avatar.getStyle()
                    .set("object-fit", "cover")
                    .set("border-radius", "8px")
                    .set("border", "1px solid #e0e0e0")
                    .set("background-color", "#f5f5f5");

            Producto producto = lote.getProducto();
            if (producto != null && producto.getFoto() != null && producto.getFoto().length > 0) {
                String base64 = Base64.getEncoder().encodeToString(producto.getFoto());
                avatar.setSrc("data:image/jpeg;base64," + base64);
            }

            Span nombreSpan = new Span(producto != null && producto.getNombre() != null ? producto.getNombre() : "Producto desconocido");
            nombreSpan.getStyle().set("font-weight", "500").set("color", "#333");

            layout.add(avatar, nombreSpan);
            return layout;
        }).setHeader("Producto").setKey("producto").setFlexGrow(1).setComparator(lote -> lote.getProducto().getNombre());

        crudLote.getGrid().addColumn(lote ->
                lote.getFechaVencimiento() != null ? lote.getFechaVencimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "No caduca"
        ).setHeader("Vencimiento").setKey("fechaVencimiento").setWidth("150px").setFlexGrow(0).setComparator(Lote::getFechaVencimiento);

        crudLote.getGrid().addComponentColumn(lote -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.addClickListener(e -> dialogLote(lote, crudLote, false));

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnEliminar.addClickListener(e -> {
                crudLote.getGrid().select(lote);
                crudLote.getDeleteButton().click();
            });

            HorizontalLayout acciones = new HorizontalLayout(btnEditar, btnEliminar);
            acciones.setSpacing(false);
            acciones.setPadding(false);
            return acciones;
        }).setHeader("Acciones").setWidth("120px").setFlexGrow(0);

        crudLote.getGrid().addThemeNames("row-stripes");

        crudLote.getAddButton().setVisible(false);
        crudLote.getUpdateButton().setVisible(false);
        crudLote.getDeleteButton().setVisible(false);
        crudLote.getFindAllButton().setVisible(false);

        Button btnNuevo = new Button("Nuevo Lote", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClassName("btn-nuevo");
        btnNuevo.addClickListener(e -> dialogLote(new Lote(), crudLote, true));

        TextField buscarLote = new TextField();
        buscarLote.setWidthFull();
        buscarLote.setPlaceholder("Buscar por N° lote o producto...");
        buscarLote.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        buscarLote.setValueChangeMode(ValueChangeMode.LAZY);
        buscarLote.addValueChangeListener(e -> {
            String filtro = e.getValue().toLowerCase().trim();
            crudLote.setFindAllOperation(() ->
                    loteService.listarTodos().stream()
                            .filter(l -> (l.getNumeroLote() != null && l.getNumeroLote().toLowerCase().contains(filtro)) ||
                                    (l.getProducto() != null && l.getProducto().getNombre().toLowerCase().contains(filtro)))
                            .toList()
            );
            crudLote.refreshGrid();
        });

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo, buscarLote);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.addClassName("almacen-toolbar");
        toolbar.expand(buscarLote);

        crudLote.setFindAllOperation(loteService::listarTodos);
        crudLote.setDeleteOperation(loteService::eliminar);

        crudLote.getCrudFormFactory().setCaption(org.vaadin.crudui.crud.CrudOperation.DELETE, "¿Eliminar Lote?");

        add(toolbar, crudLote);
    }


    private void dialogLote(Lote lote, GridCrud<Lote> crudLote, boolean esNuevo) {
        Dialog dialog = new Dialog();
        dialog.setWidth("450px");
        dialog.setCloseOnOutsideClick(false);

        H3 titulo = new H3(esNuevo ? "Registrar Nuevo Lote" : "Editar Lote");
        titulo.getStyle().set("margin", "0 0 16px 0");

        TextField txtNumeroLote = new TextField("Número de Lote (Impreso en el empaque)");
        txtNumeroLote.setWidthFull();
        txtNumeroLote.setValue(lote.getNumeroLote() != null ? lote.getNumeroLote() : "");

        ComboBox<Producto> cbProducto = new ComboBox<>("Producto");
        cbProducto.setWidthFull();
        cbProducto.setItems(productoService.listarTodosActivos());
        cbProducto.setItemLabelGenerator(Producto::getNombre);
        cbProducto.setValue(lote.getProducto());

        DatePicker dtVencimiento = new DatePicker("Fecha de Vencimiento");
        dtVencimiento.setWidthFull();
        dtVencimiento.setValue(lote.getFechaVencimiento());

        Button btnGuardar = new Button(esNuevo ? "Crear Lote" : "Guardar cambios", e -> {
            if (cbProducto.getValue() == null) {
                mostrarError("Debes seleccionar un producto.");
                return;
            }
            if (txtNumeroLote.getValue().trim().isEmpty()) {
                mostrarError("El número de lote es obligatorio.");
                return;
            }

            try {
                lote.setNumeroLote(txtNumeroLote.getValue().trim());
                lote.setProducto(cbProducto.getValue());
                lote.setFechaVencimiento(dtVencimiento.getValue());

                loteService.guardar(lote);

                dialog.close();
                crudLote.refreshGrid();

                Notification notif = Notification.show(
                        esNuevo ? "Lote registrado exitosamente" : "Lote actualizado correctamente",
                        3500, Notification.Position.BOTTOM_END);
                notif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                mostrarError("Ocurrió un error al guardar el lote.");
            }
        });
        btnGuardar.addClassName("btn-nuevo");

        Button btnCancelar = new Button("Cancelar", e -> dialog.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(btnGuardar, btnCancelar);
        botones.setWidthFull();
        botones.setJustifyContentMode(JustifyContentMode.BETWEEN);
        botones.getStyle().set("margin-top", "16px");

        VerticalLayout contenido = new VerticalLayout(titulo, txtNumeroLote, cbProducto, dtVencimiento, botones);
        contenido.setPadding(true);
        contenido.setSpacing(true);

        dialog.add(contenido);
        dialog.open();
    }

    private void mostrarError(String mensaje) {
        Notification notif = Notification.show(mensaje, 4000, Notification.Position.MIDDLE);
        notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}