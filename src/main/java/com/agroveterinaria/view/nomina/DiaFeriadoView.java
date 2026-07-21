package com.agroveterinaria.view.nomina;

import com.agroveterinaria.component.CrudGridPaginator;
import com.agroveterinaria.entity.DiaFeriado;
import com.agroveterinaria.service.DiaFeriadoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

public class DiaFeriadoView extends VerticalLayout {

    public DiaFeriadoView(DiaFeriadoService diaFeriadoService){
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        GridCrud<DiaFeriado> crudFeriados = new GridCrud<>(DiaFeriado.class, new WindowBasedCrudLayout());
        crudFeriados.getGrid().addClassName("feriado-grid");
        crudFeriados.getStyle().set("margin-top", "0");
        CrudGridPaginator<DiaFeriado> paginator = new CrudGridPaginator<>(10, "feriados");
        paginator.setRefreshOperation(crudFeriados::refreshGrid);

        crudFeriados.getGrid().removeAllColumns();

        crudFeriados.getGrid().addColumn(DiaFeriado::getFecha)
                .setHeader("Fecha").setSortable(true).setAutoWidth(true).setFlexGrow(0);
        crudFeriados.getGrid().addColumn(DiaFeriado::getDescripcion)
                .setHeader("Motivo del Feriado");

        crudFeriados.getGrid().addComponentColumn(feriado -> {
            Button btnEditar = new Button(new Icon(VaadinIcon.PENCIL));
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnEditar.getElement().setProperty("title", "Editar feriado");
            btnEditar.addClickListener(e -> {
                crudFeriados.getGrid().select(feriado);
                crudFeriados.getUpdateButton().click();
            });

            Button btnEliminar = new Button(new Icon(VaadinIcon.TRASH));
            btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            btnEliminar.getElement().setProperty("title", "Eliminar feriado");
            btnEliminar.addClickListener(e -> {
                crudFeriados.getGrid().select(feriado);
                crudFeriados.getDeleteButton().click();
            });

            HorizontalLayout acciones = new HorizontalLayout(btnEditar, btnEliminar);
            acciones.setSpacing(false);
            acciones.setPadding(false);
            return acciones;
        }).setHeader("Acciones").setWidth("100px").setFlexGrow(0);

        crudFeriados.getGrid().addThemeNames("row-stripes");

        crudFeriados.getAddButton().setVisible(false);
        crudFeriados.getUpdateButton().setVisible(false);
        crudFeriados.getDeleteButton().setVisible(false);
        crudFeriados.getFindAllButton().setVisible(false);

        Button btnNuevo = new Button("Nuevo feriado", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClassName("btn-nuevo");
        btnNuevo.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevo.addClickListener(e -> {
            crudFeriados.getAddButton().setVisible(true);
            crudFeriados.getAddButton().click();
            crudFeriados.getAddButton().setVisible(false);
        });

        HorizontalLayout toolbar = new HorizontalLayout(btnNuevo);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.getStyle().set("margin-bottom", "0");
        toolbar.getStyle().set("padding-top", "12px");

        DefaultCrudFormFactory<DiaFeriado> formFactory = (DefaultCrudFormFactory<DiaFeriado>) crudFeriados.getCrudFormFactory();
        formFactory.setUseBeanValidation(true);

        formFactory.setVisibleProperties("fecha", "descripcion");

        formFactory.setFieldProvider("fecha", df -> {
            DatePicker dp = new DatePicker("Fecha del Feriado");
            dp.setRequiredIndicatorVisible(true);
            dp.setWidthFull();
            return dp;
        });

        formFactory.setFieldProvider("descripcion", df -> {
            TextField txt = new TextField("Descripción");
            txt.setRequiredIndicatorVisible(true);
            txt.setWidthFull();
            return txt;
        });

        formFactory.setErrorListener(this::mostrarError);

        formFactory.setButtonCaption(CrudOperation.ADD, "Guardar");
        formFactory.setButtonCaption(CrudOperation.UPDATE, "Guardar cambios");
        formFactory.setButtonCaption(CrudOperation.DELETE, "Sí, eliminar");
        formFactory.setCancelButtonCaption("Cancelar");

        formFactory.setCaption(CrudOperation.ADD, "Registrar Feriado");
        formFactory.setCaption(CrudOperation.UPDATE, "Editar Feriado");
        formFactory.setCaption(CrudOperation.DELETE, "¿Eliminar Feriado?");

        paginator.setSource(diaFeriadoService::findAll);
        crudFeriados.setFindAllOperation(paginator::pageItems);
        crudFeriados.setAddOperation(diaFeriadoService::save);
        crudFeriados.setUpdateOperation(diaFeriadoService::save);
        crudFeriados.setDeleteOperation(diaFeriadoService::delete);

        add(toolbar, paginator, crudFeriados);
    }

    private void mostrarError(Exception error) {
        Throwable causa = error;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }

        String mensaje = causa.getMessage() != null && !causa.getMessage().isBlank()
                ? causa.getMessage()
                : "Ocurrió un error al procesar el feriado.";

        Notification notification = Notification.show(mensaje, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
