package com.agroveterinaria.view.compra;

import com.agroveterinaria.component.GridPaginator;
import com.agroveterinaria.entity.Compra;
import com.agroveterinaria.service.CompraService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.Setter;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
@PageTitle("Gestión de Compras")
@RolesAllowed({"ADMINISTRADOR", "ASISTENTE", "AUDITOR"})
public class ComprasView extends VerticalLayout {

    private final CompraService compraService;
    private final Grid<Compra> gridHistorial;
    private final GridPaginator<Compra> paginator;
    private final HorizontalLayout toolbar;

    @Setter
    private Consumer<Long> accionNavegarRegistro;

    public ComprasView(CompraService compraService) {
        this.compraService = compraService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.addClassName("almacen-toolbar");

        gridHistorial = new Grid<>(Compra.class, false);
        gridHistorial.setSizeFull();
        gridHistorial.addThemeNames("row-stripes");
        gridHistorial.addClassName("compra-grid");
        gridHistorial.setHeight("390px");
        paginator = new GridPaginator<>(gridHistorial, 10, "compras");

        configurarToolbar();
        configurarGrid();
        actualizarVista();

        add(toolbar, paginator, gridHistorial);
    }

    private void configurarToolbar() {
        toolbar.removeAll();

        Optional<Compra> borradorOpt = compraService.obtenerUltimoBorrador();

        if (borradorOpt.isPresent()) {
            Button btnVolverBorrador = new Button("Volver al último borrador", new Icon(VaadinIcon.ARROW_CIRCLE_UP));
            btnVolverBorrador.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnVolverBorrador.addClickListener(e -> accionNavegarRegistro.accept(borradorOpt.get().getIdCompra()));

            Button btnEliminarBorrador = new Button("Eliminar último borrador", new Icon(VaadinIcon.TRASH));
            btnEliminarBorrador.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            btnEliminarBorrador.addClickListener(e -> abrirModalConfirmacionBorrar(borradorOpt.get()));

            toolbar.add(btnVolverBorrador, btnEliminarBorrador);
        } else {
            Button btnNuevaCompra = new Button("Nueva Compra", new Icon(VaadinIcon.PLUS));
            btnNuevaCompra.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnNuevaCompra.addClickListener(e -> accionNavegarRegistro.accept(null));
            toolbar.add(btnNuevaCompra);
        }
    }

    private void configurarGrid() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        gridHistorial.addColumn(Compra::getIdCompra).setHeader("ID").setWidth("80px").setFlexGrow(0);

        gridHistorial.addColumn(compra -> compra.getFechaHoraCompra().format(formatter))
                .setHeader("Fecha / Hora").setFlexGrow(1);

        gridHistorial.addColumn(compra -> compra.getProveedor().getNombre())
                .setHeader("Proveedor").setFlexGrow(2);

        gridHistorial.addColumn(compra -> String.format("RD$ %,.2f", compra.getTotal()))
                .setHeader("Monto Total").setFlexGrow(1);

        gridHistorial.addComponentColumn(compra -> {
            Span badge = new Span(compra.getEstadoRecepcion().getEtiqueta());
            switch (compra.getEstadoRecepcion()) {
                case BORRADOR -> badge.getElement().getThemeList().add("badge");
                case PENDIENTE -> badge.getElement().getThemeList().add("badge contrast");
                case PARCIAL -> badge.getElement().getThemeList().add("badge warning");
                case RECIBIDA -> badge.getElement().getThemeList().add("badge success");
            }
            return badge;
        }).setHeader("Estado Recepción").setFlexGrow(1);

        gridHistorial.addComponentColumn(compra -> {
            HorizontalLayout acciones = new HorizontalLayout();

            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            if (compraService.tieneRecepcionesAsociadas(compra.getIdCompra())) {
                btnEditar.setEnabled(false);
                btnEliminar.setEnabled(false);
                btnEditar.setTooltipText("No se puede editar: mercancía ya recibida parcial o totalmente.");
                btnEliminar.setTooltipText("No se puede eliminar: existen movimientos de inventario vinculados.");
            } else {
                btnEditar.addClickListener(e -> accionNavegarRegistro.accept(compra.getIdCompra()));
                btnEliminar.addClickListener(e -> ejecutarEliminacionCompra(compra));
            }

            acciones.add(btnEditar, btnEliminar);
            return acciones;
        }).setHeader("Acciones").setWidth("120px").setFlexGrow(0);
    }

    private void abrirModalConfirmacionBorrar(Compra borrador) {
        Dialog modal = new Dialog();
        modal.setWidth("500px");

        H3 deatlleTitulo = new H3("Revisión de Borrador a Eliminar");

        VerticalLayout infoBorrador = new VerticalLayout(
                new Span("Proveedor: " + (borrador.getProveedor() != null ? borrador.getProveedor().getNombre() : "No seleccionado")),
                new Span(String.format("Total acumulado: RD$ %,.2f", borrador.getTotal())),
                new Span("¿Está completamente seguro de descartar este borrador? Esta acción destruirá los cambios no guardados.")
        );
        infoBorrador.setPadding(false);

        Button btnConfirmarBorrado = new Button("Sí, eliminar borrador", e -> {
            compraService.eliminarPorId(borrador.getIdCompra());
            Notification.show("Borrador descartado.", 3000, Notification.Position.BOTTOM_END);
            modal.close();
            actualizarVista();
        });
        btnConfirmarBorrado.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button btnCancelar = new Button("Cancelar", e -> modal.close());
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout layoutBotones = new HorizontalLayout(btnCancelar, btnConfirmarBorrado);
        layoutBotones.setWidthFull();
        layoutBotones.setJustifyContentMode(JustifyContentMode.BETWEEN);

        modal.add(deatlleTitulo, infoBorrador, layoutBotones);
        modal.open();
    }

    private void ejecutarEliminacionCompra(Compra compra) {
        if (!compraService.tieneRecepcionesAsociadas(compra.getIdCompra())) {
            compraService.eliminarPorId(compra.getIdCompra());
            Notification.show("Orden de compra eliminada correctamente.", 3000, Notification.Position.BOTTOM_END);
            actualizarVista();
        }
    }

    private void actualizarVista() {
        configurarToolbar();
        paginator.setItems(compraService.listarTodos());
    }
}
