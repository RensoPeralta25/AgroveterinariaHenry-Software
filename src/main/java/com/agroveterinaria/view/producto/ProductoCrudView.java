package com.agroveterinaria.view.producto;

import com.agroveterinaria.component.FotoProductoField;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.enums.CategoriaProducto;
import com.agroveterinaria.enums.UnidadMedida;
import com.agroveterinaria.service.ProductoService;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;
import com.vaadin.flow.component.combobox.ComboBox;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.math.RoundingMode;
import java.util.Base64;

@Route("productos")
@PageTitle("Gestión de Productos")
public class ProductoCrudView extends VerticalLayout {

    public ProductoCrudView(ProductoService backend) {

        GridCrud<Producto> crud = new GridCrud<>(Producto.class, new WindowBasedCrudLayout());

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
            nombreSpan.getStyle()
                    .set("font-weight", "500")
                    .set("color", "#333");

            layout.add(avatar, nombreSpan);
            return layout;
        })
                .setHeader("Producto")
                .setKey("nombre")
                .setComparator(producto -> producto.getNombre());
        crud.getGrid().addColumn(producto -> producto.getCategoria().getEtiqueta())
                .setHeader("Categoría")
                .setKey("categoria")
                .setComparator(producto -> producto.getCategoria().getEtiqueta());
        crud.getGrid().addColumn(producto -> String.format("RD$ %,.2f", producto.getPrecioUnitario()))
                .setKey("precioUnitario")
                .setHeader("Precio Unitario")
                .setComparator(producto -> producto.getPrecioUnitario());
        crud.getGrid().addColumn("presentacion").setHeader("Presentación");
        crud.getGrid().addColumn(producto -> producto.getUnidadMedida().getEtiqueta())
                .setHeader("Unidad de Medida")
                .setKey("unidadMedida")
                .setComparator(producto -> producto.getCategoria().getEtiqueta());


        DefaultCrudFormFactory<Producto> formFactory = new DefaultCrudFormFactory<>(Producto.class);

        formFactory.setUseBeanValidation(true);
        formFactory.setVisibleProperties("nombre", "precioUnitario", "categoria", "presentacion", "unidadMedida", "foto");
        formFactory.setFieldCaptions("Nombre del producto", "Precio unitario", "Categoría", "Presentación", "Unidad de medida", "");

        formFactory.setFieldCreationListener("nombre", field -> {
            TextField nombreField = (TextField) field;
            nombreField.setPlaceholder("Ej: Amoxicilina 500mg");
            nombreField.getElement().setAttribute("colspan", "2");
        });

        formFactory.setFieldCreationListener("precioUnitario", field -> {
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

        formFactory.setFieldCreationListener("presentacion", field -> {
            BigDecimalField bf = (BigDecimalField) field;
            bf.setPlaceholder("Ej: 100, 250, 2.50");
            bf.setValueChangeMode(ValueChangeMode.EAGER);
            bf.addValueChangeListener(e -> {
                if (e.getValue() != null && e.getValue().scale() > 2) {
                    bf.setValue(e.getValue().setScale(2, RoundingMode.DOWN));
                }
            });
        });

        formFactory.setFieldProvider("foto", producto -> new FotoProductoField());

        formFactory.setFieldCreationListener("foto", field -> {
            com.vaadin.flow.component.Component componenteFoto = (com.vaadin.flow.component.Component) field;
            componenteFoto.getElement().setAttribute("colspan", "2");
        });

        formFactory.setFieldProvider("categoria", producto -> {
            ComboBox<CategoriaProducto> combo = new ComboBox<>();
            combo.setItems(CategoriaProducto.values());
            combo.setItemLabelGenerator(CategoriaProducto::getEtiqueta);
            return combo;
        });

        formFactory.setFieldProvider("unidadMedida", producto -> {
            ComboBox<UnidadMedida> combo = new ComboBox<>();
            combo.setItems(UnidadMedida.values());
            combo.setItemLabelGenerator(UnidadMedida::getEtiqueta);
            return combo;
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

        crud.setFindAllOperation(() -> {
            String termino = searchField.getValue().toLowerCase().trim();
            if (termino.isEmpty()) {
                return backend.listarTodos();
            }
            return backend.listarTodos().stream()
                    .filter(producto -> {
                        boolean coincideNombre = producto.getNombre() != null &&
                                producto.getNombre().toLowerCase().contains(termino);
                        boolean coincideCategoria = producto.getCategoria() != null &&
                                producto.getCategoria().getEtiqueta().toLowerCase().contains(termino);
                        boolean coincideUnidad = producto.getUnidadMedida() != null &&
                                producto.getUnidadMedida().getEtiqueta().toLowerCase().contains(termino);
                        boolean coincidePrecio = producto.getPrecioUnitario() != null &&
                                producto.getPrecioUnitario().toString().contains(termino);
                        boolean coincidePresentacion = producto.getPresentacion() != null &&
                                producto.getPresentacion().toString().contains(termino);
                        return coincideNombre || coincideCategoria || coincideUnidad || coincidePrecio || coincidePresentacion;
                    })
                    .toList();
        });
        crud.setAddOperation(backend::guardar);
        crud.setUpdateOperation(backend::guardar);
        crud.setDeleteOperation(backend::eliminar);

        crud.setSizeFull();
        setSizeFull();
        setPadding(false);
        add(crud);
    }
}