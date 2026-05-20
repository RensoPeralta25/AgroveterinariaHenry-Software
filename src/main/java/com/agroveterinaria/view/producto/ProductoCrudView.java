package com.agroveterinaria.view.producto;

import com.agroveterinaria.component.FotoProductoField;
import com.agroveterinaria.entity.Producto;
import com.agroveterinaria.service.ProductoService;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

@Route("productos")
@PageTitle("Gestión de Productos")
public class ProductoCrudView extends VerticalLayout {

    public ProductoCrudView(ProductoService backend) {

        GridCrud<Producto> crud = new GridCrud<>(Producto.class, new WindowBasedCrudLayout());
        crud.getGrid().setColumns("nombre", "precioUnitario", "categoria", "presentacion", "unidadMedida");
        DefaultCrudFormFactory<Producto> formFactory = new DefaultCrudFormFactory<>(Producto.class);

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
        });

        formFactory.setFieldCreationListener("presentacion", field -> {
            ((BigDecimalField) field).setPlaceholder("Ej: 100, 250, 2.5");
        });

        formFactory.setFieldProvider("foto", producto -> new FotoProductoField());

        formFactory.setFieldCreationListener("foto", field -> {
            com.vaadin.flow.component.Component componenteFoto = (com.vaadin.flow.component.Component) field;
            componenteFoto.getElement().setAttribute("colspan", "2");
        });

        crud.getCrudFormFactory().setCaption(CrudOperation.ADD, "Registrar nuevo producto");
        crud.getCrudFormFactory().setCaption(CrudOperation.UPDATE, "Editar producto");

        crud.setCrudFormFactory(formFactory);

        crud.setFindAllOperation(backend::listarTodos);
        crud.setAddOperation(backend::guardar);
        crud.setUpdateOperation(backend::guardar);
        crud.setDeleteOperation(backend::eliminar);

        crud.setSizeFull();
        setSizeFull();
        setPadding(false);
        add(crud);
    }
}