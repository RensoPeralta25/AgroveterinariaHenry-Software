package com.agroveterinaria.view.producto;

import com.agroveterinaria.component.CrudGridPaginator;
import com.agroveterinaria.component.FotoProductoField;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.enums.UnidadEmpaque;
import com.agroveterinaria.service.ProductoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.math.RoundingMode;
import java.util.Base64;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
@PageTitle("Gestión de Servicios")
public class ServicioCrudView extends VerticalLayout {

    public ServicioCrudView(ProductoService backend) {

        WindowBasedCrudLayout crudLayout = new WindowBasedCrudLayout();
        crudLayout.setFormWindowWidth("500px");
        GridCrud<Producto> crud = new GridCrud<>(Producto.class, crudLayout);
        crud.addClassName("producto-crud");
        crud.getGrid().addClassName("producto-grid");
        CrudGridPaginator<Producto> paginator = new CrudGridPaginator<>(10, "servicios");
        paginator.setRefreshOperation(crud::refreshGrid);

        crud.getGrid().removeAllColumns();

        crud.getGrid().addComponentColumn(servicio -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(Alignment.CENTER);
            layout.setSpacing(true);

            Image avatar = new Image();
            avatar.setWidth("44px");
            avatar.setHeight("44px");
            avatar.getStyle().set("object-fit", "cover").set("border-radius", "8px").set("border", "1px solid #e0e0e0");

            if (servicio.getFoto() != null && servicio.getFoto().length > 0) {
                avatar.setSrc("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(servicio.getFoto()));
            }

            Span nombreSpan = new Span(servicio.getNombre() != null ? servicio.getNombre() : "");
            nombreSpan.getStyle().set("font-weight", "500");
            layout.add(avatar, nombreSpan);
            return layout;
        }).setHeader("Servicio").setKey("nombre").setFlexGrow(2).setComparator(Producto::getNombre);

        crud.getGrid().addColumn(servicio -> servicio.getPrecioEmpaque() != null ? String.format("RD$ %,.2f", servicio.getPrecioEmpaque()) : "RD$ 0.00")
                .setHeader("Tarifa / Precio")
                .setKey("precioEmpaque").setFlexGrow(1).setComparator(Producto::getPrecioEmpaque);

        crud.getGrid().addColumn(servicio -> servicio.getPorcentajeImpuesto() != null
                        ? String.format("%,.2f%%", servicio.getPorcentajeImpuesto())
                        : "0.00%")
                .setHeader("Impuesto")
                .setKey("porcentajeImpuesto")
                .setWidth("110px")
                .setFlexGrow(0)
                .setComparator(Producto::getPorcentajeImpuesto);

        crud.getGrid().addComponentColumn(servicio -> {
            boolean isActivo = servicio.getStatus() == StatusEntidad.ACTIVO;
            Span badge = new Span(servicio.getStatus() != null ? servicio.getStatus().getEtiqueta() : "");
            badge.getElement().getThemeList().add("badge " + (isActivo ? "success" : "error"));
            return badge;
        }).setHeader("Estado").setKey("status").setFlexGrow(0);

        crud.getGrid().addComponentColumn(servicio -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL), e -> { crud.getGrid().select(servicio); crud.getUpdateButton().click(); });
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH), e -> { crud.getGrid().select(servicio); crud.getDeleteButton().click(); });
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(btnEditar, btnEliminar);
        }).setHeader("Acciones").setWidth("120px").setFlexGrow(0);

        crud.getAddButton().setVisible(false);
        crud.getUpdateButton().setVisible(false);
        crud.getDeleteButton().setVisible(false);
        crud.getFindAllButton().setVisible(false);

        DefaultCrudFormFactory<Producto> formFactory = new DefaultCrudFormFactory<>(Producto.class);
        formFactory.setUseBeanValidation(true);

        formFactory.setVisibleProperties("nombre", "precioEmpaque", "porcentajeImpuesto", "foto", "status");
        formFactory.setFieldCaptions("Nombre del Servicio", "Tarifa / Precio Base", "Impuesto (%)", "Icono / Imagen", "Estado");

        formFactory.setFieldCreationListener("nombre", field -> {
            TextField nombreField = (TextField) field;
            nombreField.setPlaceholder("Ej: Consulta General, Vacunación...");
            nombreField.getElement().setAttribute("colspan", "2");
        });

        formFactory.setFieldCreationListener("precioEmpaque", field -> {
            BigDecimalField bf = (BigDecimalField) field;
            bf.setPlaceholder("0.00");
            bf.setPrefixComponent(new Span("RD$"));
            bf.setValueChangeMode(ValueChangeMode.EAGER);
        });

        formFactory.setFieldCreationListener("porcentajeImpuesto", field -> {
            BigDecimalField bf = (BigDecimalField) field;
            bf.setPlaceholder("0.00");
            bf.setSuffixComponent(new Span("%"));
            bf.setValueChangeMode(ValueChangeMode.EAGER);
            bf.addValueChangeListener(e -> {
                if (e.getValue() != null && e.getValue().scale() > 2) {
                    bf.setValue(e.getValue().setScale(2, RoundingMode.DOWN));
                }
            });
        });

        formFactory.setFieldProvider("foto", p -> new FotoProductoField());
        formFactory.setFieldCreationListener("foto", f -> ((com.vaadin.flow.component.Component) f).getElement().setAttribute("colspan", "2"));

        formFactory.setFieldProvider("status", p -> {
            ComboBox<StatusEntidad> cbStatus = new ComboBox<>();
            cbStatus.setItems(StatusEntidad.values());
            cbStatus.setItemLabelGenerator(StatusEntidad::getEtiqueta);
            cbStatus.setValue(((Producto) p).getStatus() != null ? ((Producto) p).getStatus() : StatusEntidad.ACTIVO);
            cbStatus.getElement().setAttribute("colspan", "2");
            return cbStatus;
        });

        crud.getCrudFormFactory().setCaption(CrudOperation.ADD, "Registrar nuevo servicio");
        crud.getCrudFormFactory().setCaption(CrudOperation.UPDATE, "Editar servicio");
        formFactory.setCancelButtonCaption("Cancelar");
        crud.setCrudFormFactory(formFactory);

        Button btnNuevo = new Button("Nuevo Servicio", new Icon(VaadinIcon.PLUS), e -> crud.getAddButton().click());
        btnNuevo.addClassName("btn-nuevo");

        TextField searchField = new TextField();
        searchField.setPlaceholder("Buscar servicio...");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> paginator.reset());

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo, searchField);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.expand(searchField);

        paginator.setSource(() -> {
            String termino = searchField.getValue().toLowerCase().trim();
            return backend.listarTodos().stream()
                    .filter(p -> p.getCategoria() == CategoriaProducto.SERVICIO)
                    .filter(p -> termino.isEmpty() || (p.getNombre() != null && p.getNombre().toLowerCase().contains(termino)))
                    .toList();
        });
        crud.setFindAllOperation(paginator::pageItems);

        crud.setAddOperation(servicio -> {
            servicio.setCategoria(CategoriaProducto.SERVICIO);
            servicio.setPermiteFraccionamiento(false);
            servicio.setUnidadEmpaque(UnidadEmpaque.UNIDAD_COMPLETA);
            return backend.guardar(servicio);
        });

        crud.setUpdateOperation(servicio -> backend.guardar(servicio));
        crud.setDeleteOperation(backend::eliminar);

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        add(toolbar, paginator, crud);
    }
}
