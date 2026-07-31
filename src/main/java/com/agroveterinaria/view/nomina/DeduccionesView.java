package com.agroveterinaria.view.nomina;

import com.agroveterinaria.entity.*;
import com.agroveterinaria.security.SecurityService;
import com.agroveterinaria.service.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class DeduccionesView extends VerticalLayout {

    private final EmbargoSalarialService embargoSalarialService;
    private final AnticipoSalarioService anticipoSalarioService;
    private final PrestamoEmpleadoService prestamoEmpleadoService;
    private final EmpleadoService empleadoService;
    private final ConfiguracionNominaService configuracionNominaService;
    private final CuentaBancariaTransferenciaPdfService cuentaBancariaTransferenciaPdfService;
    private final SecurityService securityService;


    public DeduccionesView(EmbargoSalarialService embargoSalarialService, AnticipoSalarioService anticipoSalarioService,
                           PrestamoEmpleadoService prestamoEmpleadoService, EmpleadoService empleadoService, ConfiguracionNominaService configuracionNominaService,
                           CuentaBancariaTransferenciaPdfService cuentaBancariaTransferenciaPdfService, SecurityService securityService) {
        this.embargoSalarialService = embargoSalarialService;
        this.anticipoSalarioService = anticipoSalarioService;
        this.prestamoEmpleadoService = prestamoEmpleadoService;
        this.empleadoService = empleadoService;
        this.configuracionNominaService = configuracionNominaService;
        this.cuentaBancariaTransferenciaPdfService = cuentaBancariaTransferenciaPdfService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        Tab tabEmbargos = new Tab("Embargos Salariales");
        Tab separador1 = crearSeparador();
        Tab tabPrestamos = new Tab("Préstamos");
        Tab separador2 = crearSeparador();
        Tab tabAnticipos = new Tab("Anticipos de Salario");

        Tabs tabs = new Tabs(tabAnticipos, separador1, tabPrestamos, separador2, tabEmbargos);
        tabs.setWidthFull();
        tabs.getStyle().set("border-bottom", "1px solid #e0e0e0");
        tabs.getStyle().set("width", "fit-content");

        VerticalLayout contenidoEmbargos = new EmbargoSalarialView(embargoSalarialService,empleadoService, configuracionNominaService, anticipoSalarioService);
        VerticalLayout contenidoPrestamos = new PrestamoEmpleadoView(prestamoEmpleadoService, empleadoService, cuentaBancariaTransferenciaPdfService, securityService);
        VerticalLayout contenidoAnticipos = new AnticipoSalarioView(anticipoSalarioService,empleadoService, cuentaBancariaTransferenciaPdfService, securityService);

        contenidoPrestamos.setVisible(false);
        contenidoEmbargos.setVisible(false);

        tabs.addSelectedChangeListener(e -> {
            Tab selected = tabs.getSelectedTab();
            contenidoEmbargos.setVisible(selected.equals(tabEmbargos));
            contenidoPrestamos.setVisible(selected.equals(tabPrestamos));
            contenidoAnticipos.setVisible(selected.equals(tabAnticipos));
        });

        add(tabs, contenidoEmbargos, contenidoPrestamos, contenidoAnticipos);
    }

    private Tab crearSeparador() {
        Tab separador = new Tab("|");
        separador.getStyle()
                .set("color", "#cccccc")
                .set("pointer-events", "none")
                .set("cursor", "default")
                .set("padding", "0 4px")
                .set("min-width", "0");
        separador.setEnabled(false);
        return separador;
    }

}