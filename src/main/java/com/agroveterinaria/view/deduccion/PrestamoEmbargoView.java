package com.agroveterinaria.view.deduccion;

import com.agroveterinaria.component.CrudGridPaginator;
import com.agroveterinaria.entity.EmbargoSalarial;
import com.agroveterinaria.entity.PrestamoEmpleado;
import com.agroveterinaria.service.EmbargoSalarialService;
import com.agroveterinaria.service.EmpleadoService;
import com.agroveterinaria.service.PrestamoEmpleadoService;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import org.vaadin.crudui.crud.impl.GridCrud;

public class PrestamoEmbargoView extends VerticalLayout {
    private static final int ITEMS_POR_PAGINA = 10;

    public PrestamoEmbargoView(EmpleadoService empleadoService, PrestamoEmpleadoService prestamoService, EmbargoSalarialService embargoService) {
        setSizeFull();
        setPadding(true);
        setSpacing(false);

        Tab tabPrestamos = new Tab("Préstamos Internos");
        Tab tabEmbargos = new Tab("Embargos");

        Tab separador = new Tab("|");
        separador.getStyle()
                .set("color", "#cccccc")
                .set("pointer-events", "none")
                .set("cursor", "default")
                .set("padding", "0 4px")
                .set("min-width", "0");
        separador.setEnabled(false);

        Tabs tabs = new Tabs(tabPrestamos, separador, tabEmbargos);
        tabs.setWidthFull();
        tabs.getStyle().set("border-bottom", "1px solid #e0e0e0");
        tabs.getStyle().set("width", "fit-content");

        VerticalLayout vistaPrestamos = construirVistaPrestamos(prestamoService);
        VerticalLayout vistaEmbargos = construirVistaEmbargos(embargoService);

        vistaEmbargos.setVisible(false);

        tabs.addSelectedChangeListener(e -> {
            Tab selected = tabs.getSelectedTab();
            vistaPrestamos.setVisible(selected.equals(tabPrestamos));
            vistaEmbargos.setVisible(selected.equals(tabEmbargos));
        });

        add(tabs, vistaPrestamos, vistaEmbargos);
    }

    private VerticalLayout construirVistaPrestamos(PrestamoEmpleadoService prestamoService) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);

        GridCrud<PrestamoEmpleado> crudPrestamo = new GridCrud<>(PrestamoEmpleado.class);
        CrudGridPaginator<PrestamoEmpleado> paginator = new CrudGridPaginator<>(ITEMS_POR_PAGINA, "prestamos");
        paginator.setRefreshOperation(crudPrestamo::refreshGrid);
        paginator.setSource(prestamoService::findAll);
        crudPrestamo.setFindAllOperation(paginator::pageItems);
        // Aquí configuraremos el Grid de préstamos (columnas, cuotas, balance, etc.)
        // crudPrestamo.getGrid().removeAllColumns(); ...

        layout.add(paginator, crudPrestamo);
        return layout;
    }

    private VerticalLayout construirVistaEmbargos(EmbargoSalarialService embargoService) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);

        GridCrud<EmbargoSalarial> crudEmbargo = new GridCrud<>(EmbargoSalarial.class);
        CrudGridPaginator<EmbargoSalarial> paginator = new CrudGridPaginator<>(ITEMS_POR_PAGINA, "embargos");
        paginator.setRefreshOperation(crudEmbargo::refreshGrid);
        paginator.setSource(embargoService::findAll);
        crudEmbargo.setFindAllOperation(paginator::pageItems);



        layout.add(paginator, crudEmbargo);
        return layout;
    }
}
