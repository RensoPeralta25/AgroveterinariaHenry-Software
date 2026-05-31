package com.agroveterinaria.view.producto;

import com.agroveterinaria.component.FotoProductoField;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.StatusEntidad;
import com.agroveterinaria.enums.UnidadEmpaque;
import com.agroveterinaria.enums.UnidadMedida;
import com.agroveterinaria.service.ProductoService;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
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
import org.vaadin.crudui.crud.CrudOperationException;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.math.RoundingMode;
import java.util.Base64;

@Route("productos")
@PageTitle("Gestión de Productos")
public class ProductoCrudView extends VerticalLayout {

    public ProductoCrudView(ProductoService backend) {

        WindowBasedCrudLayout crudLayout = new WindowBasedCrudLayout();
        crudLayout.setFormWindowWidth("600px");
        GridCrud<Producto> crud = new GridCrud<>(Producto.class, crudLayout);

        crud.getGrid().removeAllColumns();
        crud.getGrid().addComponentColumn(producto -> {
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

            if (producto.getFoto() != null && producto.getFoto().length > 0) {
                String base64 = Base64.getEncoder().encodeToString(producto.getFoto());
                avatar.setSrc("data:image/jpeg;base64," + base64);
            }

            Span nombreSpan = new Span(producto.getNombre() != null ? producto.getNombre() : "");
            nombreSpan.getStyle().set("font-weight", "500").set("color", "#333");

            layout.add(avatar, nombreSpan);
            return layout;
        }).setHeader("Producto").setKey("nombre").setComparator(producto -> producto.getNombre());

        crud.getGrid().addColumn(producto -> producto.getCategoria() != null ? producto.getCategoria().getEtiqueta() : "")
                .setHeader("Categoría")
                .setKey("categoria")
                .setComparator(producto -> producto.getCategoria().getEtiqueta());

        crud.getGrid().addColumn(producto -> producto.getUnidadEmpaque() != null ? producto.getUnidadEmpaque().getEtiqueta() : "")
                .setHeader("Empaque")
                .setKey("unidadEmpaque")
                .setComparator(producto -> producto.getUnidadEmpaque().getEtiqueta());

        crud.getGrid().addColumn(producto -> producto.getPrecioEmpaque() != null ? String.format("RD$ %,.2f", producto.getPrecioEmpaque()) : "RD$ 0.00")
                .setHeader("Precio Empaque")
                .setKey("precioEmpaque")
                .setComparator(Producto::getPrecioEmpaque);

        crud.getGrid().addComponentColumn(producto -> {
            boolean isActivo = producto.getStatus() == StatusEntidad.ACTIVO;
            Span badge = new Span(producto.getStatus() != null ? producto.getStatus().getEtiqueta() : "");
            badge.getElement().getThemeList().add("badge " + (isActivo ? "success" : "error"));
            return badge;
        }).setHeader("Estado").setKey("status").setComparator(producto -> producto.getStatus().getEtiqueta());

        crud.getGrid().addColumn(producto -> (producto.getPermiteFraccionamiento() != null && producto.getPermiteFraccionamiento()) ? "Sí" : "No")
                .setHeader("Al Detalle")
                .setKey("permiteFraccionamiento");



        DefaultCrudFormFactory<Producto> formFactory = new DefaultCrudFormFactory<>(Producto.class);

        formFactory.setUseBeanValidation(true);

        formFactory.setVisibleProperties(
                "nombre", "categoria", "unidadEmpaque", "precioEmpaque",
                "status", "permiteFraccionamiento", "contenidoPorEmpaque", "unidadFraccion", "precioFraccion", "foto"
        );

        formFactory.setFieldCaptions(
                "Nombre del producto", "Categoría", "Formato de Almacén (Empaque)", "Precio por Empaque",
                "Estado del Producto", "¿Permite venta al detalle?", "Fracciones/Unidades por empaque", "Unidad al Detalle", "Precio Fracción", ""
        );


        formFactory.setFieldCreationListener("nombre", field -> {
            TextField nombreField = (TextField) field;
            nombreField.setPlaceholder("Ej: Amoxicilina 500mg");
            nombreField.getElement().setAttribute("colspan", "2");
        });

        formFactory.setFieldProvider("categoria", producto -> {
            ComboBox<CategoriaProducto> combo = new ComboBox<>();
            combo.setItems(CategoriaProducto.values());
            combo.setItemLabelGenerator(CategoriaProducto::getEtiqueta);
            return combo;
        });

        formFactory.setFieldProvider("unidadEmpaque", producto -> {
            ComboBox<UnidadEmpaque> combo = new ComboBox<>();
            combo.setItems(UnidadEmpaque.values());
            combo.setItemLabelGenerator(UnidadEmpaque::getEtiqueta);
            return combo;
        });

        formFactory.setFieldCreationListener("precioEmpaque", field -> {
            BigDecimalField bf = (BigDecimalField) field;
            bf.setPlaceholder("0.00");
            bf.setPrefixComponent(new Span("RD$"));
            bf.setValueChangeMode(ValueChangeMode.EAGER);
            bf.addValueChangeListener(e -> {
                if (e.getValue() != null && e.getValue().scale() > 2) {
                    bf.setValue(e.getValue().setScale(2, RoundingMode.DOWN));
                }
            });
        });


        Checkbox chkPermiteFraccion = new Checkbox("¿Permite venta al detalle (fraccionada)?");
        chkPermiteFraccion.getElement().setAttribute("colspan", "2");

        BigDecimalField txtContenido = new BigDecimalField();
        txtContenido.setPlaceholder("Ej: 100, 250");
        txtContenido.setValueChangeMode(ValueChangeMode.EAGER);
        txtContenido.addValueChangeListener(e -> {
            if (e.getValue() != null && e.getValue().scale() > 2) {
                txtContenido.setValue(e.getValue().setScale(2, RoundingMode.DOWN));
            }
        });

        ComboBox<UnidadMedida> cbUnidadFraccion = new ComboBox<>();
        cbUnidadFraccion.setItems(UnidadMedida.values());
        cbUnidadFraccion.setItemLabelGenerator(UnidadMedida::getEtiqueta);

        BigDecimalField txtPrecioFraccion = new BigDecimalField();
        txtPrecioFraccion.setPlaceholder("0.00");
        txtPrecioFraccion.setPrefixComponent(new Span("RD$"));
        txtPrecioFraccion.setValueChangeMode(ValueChangeMode.EAGER);
        txtPrecioFraccion.addValueChangeListener(e -> {
            if (e.getValue() != null && e.getValue().scale() > 2) {
                txtPrecioFraccion.setValue(e.getValue().setScale(2, RoundingMode.DOWN));
            }
        });


        Runnable aplicarVisibilidad = () -> {
            boolean permite = chkPermiteFraccion.getValue() != null && chkPermiteFraccion.getValue();

            txtContenido.setVisible(permite);
            txtContenido.setRequiredIndicatorVisible(permite);

            cbUnidadFraccion.setVisible(permite);
            cbUnidadFraccion.setRequiredIndicatorVisible(permite);

            txtPrecioFraccion.setVisible(permite);
            txtPrecioFraccion.setRequiredIndicatorVisible(permite);

            if (!permite) {
                txtContenido.clear();
                cbUnidadFraccion.clear();
                txtPrecioFraccion.clear();
            }
        };

        chkPermiteFraccion.addValueChangeListener(e -> aplicarVisibilidad.run());

        formFactory.setFieldProvider("permiteFraccionamiento", p -> {
            chkPermiteFraccion.setValue(((Producto) p).getPermiteFraccionamiento() != null && ((Producto) p).getPermiteFraccionamiento());
            aplicarVisibilidad.run();
            return chkPermiteFraccion;
        });

        formFactory.setFieldProvider("contenidoPorEmpaque", p -> txtContenido);
        formFactory.setFieldProvider("unidadFraccion", p -> cbUnidadFraccion);
        formFactory.setFieldProvider("precioFraccion", p -> txtPrecioFraccion);


        formFactory.setFieldProvider("foto", producto -> new FotoProductoField());

        formFactory.setFieldCreationListener("foto", field -> {
            com.vaadin.flow.component.Component componenteFoto = (com.vaadin.flow.component.Component) field;
            componenteFoto.getElement().setAttribute("colspan", "2");
        });

        formFactory.setFieldProvider("status", p -> {
            ComboBox<StatusEntidad> cbStatus = new ComboBox<>();
            cbStatus.setItems(StatusEntidad.values());
            cbStatus.setItemLabelGenerator(StatusEntidad::getEtiqueta);
            Producto productoActual = (Producto) p;
            cbStatus.setValue(productoActual.getStatus() != null ? productoActual.getStatus() : StatusEntidad.ACTIVO);
            cbStatus.getElement().setAttribute("colspan", "2");
            return cbStatus;
        });

        formFactory.setErrorListener(e -> {
            Notification alerta = Notification.show(
                    e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
            );
            alerta.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        crud.getCrudFormFactory().setCaption(CrudOperation.ADD, "Registrar nuevo producto");
        crud.getCrudFormFactory().setCaption(CrudOperation.UPDATE, "Editar producto");

        crud.setCrudFormFactory(formFactory);


        TextField searchField = new TextField();
        searchField.setPlaceholder("Buscar producto...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> crud.refreshGrid());
        crud.getCrudLayout().addFilterComponent(searchField);

        ComboBox<StatusEntidad> statusFilter = new ComboBox<>();
        statusFilter.setPlaceholder("Todos los estados");
        statusFilter.setItems(StatusEntidad.values());
        statusFilter.setItemLabelGenerator(StatusEntidad::getEtiqueta);
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> crud.refreshGrid());
        crud.getCrudLayout().addFilterComponent(statusFilter);
        crud.setFindAllOperation(() -> {
            String termino = searchField.getValue().toLowerCase().trim();
            StatusEntidad estadoSeleccionado = statusFilter.getValue();

            return backend.listarTodos().stream()
                    .filter(producto -> {
                        if (estadoSeleccionado != null && producto.getStatus() != estadoSeleccionado) {
                            return false;
                        }

                        if (!termino.isEmpty()) {
                            boolean coincideNombre = producto.getNombre() != null && producto.getNombre().toLowerCase().contains(termino);
                            boolean coincideCategoria = producto.getCategoria() != null && producto.getCategoria().getEtiqueta().toLowerCase().contains(termino);
                            boolean coincideEmpaque = producto.getUnidadEmpaque() != null && producto.getUnidadEmpaque().getEtiqueta().toLowerCase().contains(termino);
                            boolean coincidePrecio = producto.getPrecioEmpaque() != null && producto.getPrecioEmpaque().toString().contains(termino);

                            return coincideNombre || coincideCategoria || coincideEmpaque || coincidePrecio;
                        }

                        return true;
                    })
                    .toList();
        });

        crud.setAddOperation(producto -> {
            validarFraccionamiento(producto);
            return backend.guardar(producto);
        });

        crud.setUpdateOperation(producto -> {
            validarFraccionamiento(producto);
            return backend.guardar(producto);
        });

        crud.setDeleteOperation(backend::eliminar);

        crud.setSizeFull();
        setSizeFull();
        setPadding(false);
        add(crud);
    }


    // Auxiliares

    private void validarFraccionamiento(Producto producto) {
        if (producto.getPermiteFraccionamiento() != null && producto.getPermiteFraccionamiento()) {
            if (producto.getContenidoPorEmpaque() == null ||
                    producto.getUnidadFraccion() == null ||
                    producto.getPrecioFraccion() == null) {

                throw new CrudOperationException("DEBE COMPLETAR TODOS LOS CAMPOS DE 'VENTA AL DETALLE' O DESMARCAR LA CASILLA.");
            }
        }
    }
}