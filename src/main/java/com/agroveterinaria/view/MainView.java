package com.agroveterinaria.view;

import com.agroveterinaria.entity.Empleado;
import com.agroveterinaria.entity.Persona;
import com.agroveterinaria.entity.Usuario;
import com.agroveterinaria.security.SecurityService;
import com.agroveterinaria.service.EmpleadoService;
import com.agroveterinaria.service.ProductoService;
import com.agroveterinaria.service.ProveedorService;
import com.agroveterinaria.service.UsuarioService;
import com.agroveterinaria.view.almacen.AjustesInventarioView;
import com.agroveterinaria.view.almacen.AlmacenView;
import com.agroveterinaria.view.almacen.GestionRecepcionesView;
import com.agroveterinaria.view.almacen.InventarioGlobalView;
import com.agroveterinaria.view.compra.ComprasView;
import com.agroveterinaria.view.compra.RegistroCompraView;
import com.agroveterinaria.view.devolucion.DevolucionesView;
import com.agroveterinaria.view.devolucion.RegistroDevolucionView;
import com.agroveterinaria.view.empleado.EmpleadoView;
import com.agroveterinaria.service.*;
import com.agroveterinaria.view.cita.CitaView;
import com.agroveterinaria.view.cliente.ClienteView;
import com.agroveterinaria.view.cobro.CobroView;
import com.agroveterinaria.view.configuracion.ConfiguracionUsuarioView;
import com.agroveterinaria.view.dashboard.DashboardView;
import com.agroveterinaria.view.finanzas.GastosView;
import com.agroveterinaria.view.logistica.GestionDespachosView;
import com.agroveterinaria.view.logistica.RutaView;
import com.agroveterinaria.view.lote.LoteView;
import com.agroveterinaria.view.mascota.MascotaView;
import com.agroveterinaria.view.nomina.AnticipoSalarioView;
import com.agroveterinaria.view.nomina.DeduccionesView;
import com.agroveterinaria.view.nomina.NominaView;
import com.agroveterinaria.view.producto.ProductoCrudView;
import com.agroveterinaria.view.producto.ServicioCrudView;
import com.agroveterinaria.view.proveedor.ProveedorView;
import com.agroveterinaria.view.Venta.ListaVentasView;
import com.agroveterinaria.view.Venta.VentaView;
import com.agroveterinaria.view.transferencia.RegistroTransferenciaView;
import com.agroveterinaria.view.transferencia.TransferenciasView;
import com.agroveterinaria.view.transporte.VehiculoView;
import com.agroveterinaria.view.usuario.UsuarioView;
import com.agroveterinaria.view.vacacion.VacacionEmpleadoView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Route("")
@PageTitle("Agroveterinaria Henry | Panel principal")
@PermitAll
public class MainView extends Div {

    private final Div contentArea = new Div();
    private final H2 moduleTitle = new H2();
    private final List<Button> menuButtons = new ArrayList<>();

    private final transient AuthenticationContext authContext;
    private final PasswordEncoder passwordEncoder;

    public MainView(
            UsuarioService usuarioService,
            ProductoService productoService,
            ProveedorService proveedorService,
            EmpleadoService empleadoService,
            ClienteService clienteService,
            CitaService citaService,
            MascotaService mascotaService,
            VentaService ventaService,
            AuthenticationContext authContext,
            PersonaService personaService,
            PasswordEncoder passwordEncoder,
            CorridaNominaService corridaNominaService,
            DetalleNominaService detalleNominaService,
            ConfiguracionNominaService configuracionNominaService,
            AlmacenService almacenService,
            InventarioService inventarioService,
            AjusteInventarioService ajusteInventarioService,
            LoteService loteService,
            SecurityService securityService,
            CompraService compraService,
            RecepcionService recepcionService,
            DespachoService despachoService,
            TransferenciaService transferenciaService,
            VehiculoService vehiculoService,
            RutaService rutaService,
            FacturaVentaPdfService facturaVentaPdfService,
            CuentaBancariaTransferenciaPdfService cuentaBancariaTransferenciaPdfService,
            DashboardService dashboardService,
            GastoOperativoService gastoOperativoService,
            NominaService nominaService,
            VacacionEmpleadoService vacacionEmpleadoService,
            DiaFeriadoService diaFeriadoService,
            PeriodoFiscalService periodoFiscalService,
            AnticipoSalarioService anticipoSalarioService,
            DevolucionVentaService devolucionVentaService,
            TransporteService transporteService,
            EmbargoSalarialService embargoSalarialService,
            PrestamoEmpleadoService prestamoEmpleadoService) {
        this.authContext = authContext;
        this.passwordEncoder = passwordEncoder;

        addClassName("main-view");
        setSizeFull();

        VerticalLayout sidebar = createSidebar(usuarioService, productoService, proveedorService, empleadoService,
                clienteService, citaService, mascotaService, personaService, ventaService, corridaNominaService,
                detalleNominaService, configuracionNominaService, almacenService, inventarioService,
                ajusteInventarioService, loteService, securityService, compraService, recepcionService,
                despachoService, transferenciaService, vehiculoService, rutaService, facturaVentaPdfService,
                cuentaBancariaTransferenciaPdfService, dashboardService,
                gastoOperativoService, nominaService, vacacionEmpleadoService, diaFeriadoService, periodoFiscalService,
                anticipoSalarioService, devolucionVentaService, transporteService, embargoSalarialService, prestamoEmpleadoService);
      
        HorizontalLayout shell = new HorizontalLayout(sidebar);
        shell.addClassName("app-shell");
        shell.setSizeFull();
        shell.setSpacing(false);
        shell.setPadding(false);

        VerticalLayout mainPanel = createMainPanel(securityService, shell);
        shell.add(mainPanel);
        shell.expand(mainPanel);

        add(shell);
        showModule(
                menuButtons.get(0),
                "Inicio",
                "Panel Principal",
                "Resumen general de la operación administrativa.",
                VaadinIcon.DASHBOARD,
                new DashboardView(dashboardService)
        );
    }

    private VerticalLayout createSidebar(
            UsuarioService usuarioService,
            ProductoService productoService,
            ProveedorService proveedorService,
            EmpleadoService empleadoService,
            ClienteService clienteService,
            CitaService citaService,
            MascotaService mascotaService,
            PersonaService personaService,
            VentaService ventaService,
            CorridaNominaService corridaNominaService,
            DetalleNominaService detalleNominaService,
            ConfiguracionNominaService configuracionNominaService,
            AlmacenService almacenService,
            InventarioService inventarioService,
            AjusteInventarioService ajusteInventarioService,
            LoteService loteService,
            SecurityService securityService,
            CompraService compraService,
            RecepcionService recepcionService,
            DespachoService despachoService,
            TransferenciaService transferenciaService,
            VehiculoService vehiculoService,
            RutaService rutaService,
            FacturaVentaPdfService facturaVentaPdfService,
            CuentaBancariaTransferenciaPdfService cuentaBancariaTransferenciaPdfService,
            DashboardService dashboardService,
            GastoOperativoService gastoOperativoService,
            NominaService nominaService,
            VacacionEmpleadoService vacacionEmpleadoService,
            DiaFeriadoService diaFeriadoService,
            PeriodoFiscalService periodoFiscalService,
            AnticipoSalarioService anticipoSalarioService,
            DevolucionVentaService devolucionVentaService,
            TransporteService transporteService,
            EmbargoSalarialService embargoSalarialService,
            PrestamoEmpleadoService prestamoEmpleadoService
    ) {
        Div logoMark = new Div();
        logoMark.addClassName("brand-mark");
        logoMark.setText("AH");

        H1 brandName = new H1("Agroveterinaria Henry");
        brandName.addClassName("brand-name");

        Span brandSubtitle = new Span("Panel administrativo");
        brandSubtitle.addClassName("brand-subtitle");

        VerticalLayout brandText = new VerticalLayout(brandName, brandSubtitle);
        brandText.setPadding(false);
        brandText.setSpacing(false);

        HorizontalLayout brand = new HorizontalLayout(logoMark, brandText);
        brand.addClassName("brand");
        brand.setAlignItems(FlexComponent.Alignment.CENTER);

        boolean esAdmin = authContext.hasRole("ADMINISTRADOR");
        boolean esCajero = authContext.hasRole("CAJERO");
        boolean esVeterinario = authContext.hasRole("VETERINARIO");
        boolean esConductor = authContext.hasRole("CONDUCTOR");
        boolean esAsistente = authContext.hasRole("ASISTENTE");

        Button inicioButton = createMenuButton(VaadinIcon.HOME, "Inicio");
        Button productosButton = createMenuButton(VaadinIcon.PACKAGE, "Productos");
        Button serviciosButton = createMenuButton(VaadinIcon.STETHOSCOPE, "Servicios");
        Button proveedoresButton = createMenuButton(VaadinIcon.TRUCK, "Proveedores");
        Button usuariosButton = createMenuButton(VaadinIcon.USERS, "Usuarios");
        Button empleadosButton = createSubmenuButton(VaadinIcon.GROUP, "Empleados");
        Button clientesButton = createMenuButton(VaadinIcon.USER, "Clientes");
        Button mascotasButton = createMenuButton(VaadinIcon.HEART, "Mascotas");
        Button citasButton = createMenuButton(VaadinIcon.CALENDAR, "Citas");
        Button ventasButton = createMenuButton(VaadinIcon.CART, "Ventas");
        Button recursosHumanosButton = createMenuButton(VaadinIcon.INVOICE, "Recursos Humanos");
        Button nominaButton = createSubmenuButton(VaadinIcon.INVOICE, "Nómina");
        Button vacacionesButton = createSubmenuButton(VaadinIcon.FLIGHT_TAKEOFF, "Vacaciones");
        Button deduccionesButton = createSubmenuButton(VaadinIcon.MONEY_WITHDRAW, "Deducciones");
        Button registrarVentaButton = createSubmenuButton(VaadinIcon.PLUS, "Registrar venta");
        Button listaVentasButton = createSubmenuButton(VaadinIcon.LIST, "Lista de ventas");
        Button cobrosButton = createSubmenuButton(VaadinIcon.MONEY, "Cobros");
        Button devolucionesBtn = createSubmenuButton(VaadinIcon.RECYCLE, "Devoluciones");
        Button almacenButton = createMenuButton(VaadinIcon.STOCK, "Almacén e Inventario");
        Button logisticaButton = createMenuButton(VaadinIcon.ROAD, "Logística");
        Button inventarioGlobalBtn = createSubmenuButton(VaadinIcon.GLOBE, "Inventario Global");
        Button gestionAlmacenesBtn = createSubmenuButton(VaadinIcon.BUILDING, "Gestión de Almacenes");
        Button controlLotesBtn = createSubmenuButton(VaadinIcon.BARCODE, "Control de Lotes");
        Button ajustesAuditoriaBtn = createSubmenuButton(VaadinIcon.ADJUST, "Ajustes / Auditoría");
        VerticalLayout almacenSubmenu = new VerticalLayout(inventarioGlobalBtn, gestionAlmacenesBtn, controlLotesBtn, ajustesAuditoriaBtn);
        almacenSubmenu.addClassName("sidebar-submenu");
        almacenSubmenu.setPadding(false);
        almacenSubmenu.setSpacing(false);
        almacenSubmenu.setVisible(false);
        Button historialComprasBtn = createSubmenuButton(VaadinIcon.SHOP, "Gestión de Compras");
        Button recepcionesBtn = createSubmenuButton(VaadinIcon.INBOX, "Gestión de Recepciones");
        Button despachosBtn = createSubmenuButton(VaadinIcon.OUTBOX, "Gestión de Despachos");
        Button regTransferenciaBtn = createSubmenuButton(VaadinIcon.EXCHANGE, "Transferencias");
        Button vehiculosBtn = createSubmenuButton(VaadinIcon.TRUCK, "Flota de Vehículos");
        Button rutaBtn = createSubmenuButton(VaadinIcon.ROAD, "Configuración de Rutas");

        VerticalLayout logisticaSubmenu = new VerticalLayout(historialComprasBtn, recepcionesBtn, despachosBtn, regTransferenciaBtn, vehiculosBtn, rutaBtn);
        logisticaSubmenu.addClassName("sidebar-submenu");
        logisticaSubmenu.setPadding(false);
        logisticaSubmenu.setSpacing(false);
        logisticaSubmenu.setVisible(false);

        VerticalLayout ventasSubmenu = new VerticalLayout(registrarVentaButton, listaVentasButton, cobrosButton, devolucionesBtn);
        ventasSubmenu.addClassName("sidebar-submenu");
        ventasSubmenu.setPadding(false);
        ventasSubmenu.setSpacing(false);
        ventasSubmenu.setVisible(false);

        VerticalLayout recursosHumanosSubMenu = new VerticalLayout(empleadosButton, nominaButton, vacacionesButton, deduccionesButton);
        recursosHumanosSubMenu.addClassName("sidebar-submenu");
        recursosHumanosSubMenu.setPadding(false);
        recursosHumanosSubMenu.setSpacing(false);
        recursosHumanosSubMenu.setVisible(false);

        Button finanzasButton = createMenuButton(VaadinIcon.MONEY_EXCHANGE, "Finanzas");
        Button gastosGeneralesBtn = createSubmenuButton(VaadinIcon.MONEY_WITHDRAW, "Gastos Operativos");

        VerticalLayout finanzasSubmenu = new VerticalLayout(gastosGeneralesBtn);
        finanzasSubmenu.addClassName("sidebar-submenu");
        finanzasSubmenu.setPadding(false);
        finanzasSubmenu.setSpacing(false);
        finanzasSubmenu.setVisible(false);


        inicioButton.setEnabled(true);

        clientesButton.setEnabled(esAdmin || esCajero || esVeterinario);
        ventasButton.setEnabled(esAdmin || esCajero);
        registrarVentaButton.setEnabled(esAdmin || esCajero);
        listaVentasButton.setEnabled(esAdmin || esCajero);
        cobrosButton.setEnabled(esAdmin || esCajero);
        devolucionesBtn.setEnabled(esAdmin || esCajero);

        mascotasButton.setEnabled(esAdmin || esVeterinario);
        citasButton.setEnabled(esAdmin || esVeterinario);

        productosButton.setEnabled(esAdmin);
        serviciosButton.setEnabled(esAdmin);
        proveedoresButton.setEnabled(esAdmin);
        usuariosButton.setEnabled(esAdmin);
        recursosHumanosButton.setEnabled(esAdmin);
        empleadosButton.setEnabled(esAdmin);
        nominaButton.setEnabled(esAdmin);
        vacacionesButton.setEnabled(esAdmin);
        deduccionesButton.setEnabled(esAdmin);

        boolean accesoAlmacen = esAdmin || esAsistente;

        almacenButton.setEnabled(accesoAlmacen);
        inventarioGlobalBtn.setEnabled(accesoAlmacen);
        gestionAlmacenesBtn.setEnabled(esAdmin);
        controlLotesBtn.setEnabled(accesoAlmacen);
        ajustesAuditoriaBtn.setEnabled(accesoAlmacen);

        logisticaButton.setEnabled(accesoAlmacen || esConductor);
        historialComprasBtn.setEnabled(accesoAlmacen);
        regTransferenciaBtn.setEnabled(accesoAlmacen);
        recepcionesBtn.setEnabled(accesoAlmacen || esConductor);
        despachosBtn.setEnabled(accesoAlmacen || esConductor);
        vehiculosBtn.setEnabled(esAdmin);

        finanzasButton.setEnabled(esAdmin);
        gastosGeneralesBtn.setEnabled(esAdmin);


        inicioButton.addClickListener(event -> showModule(
                inicioButton,
                "Inicio",
                "Panel Principal",
                "Resumen general de la operación administrativa.",
                VaadinIcon.DASHBOARD,
                new DashboardView(dashboardService)
        ));

        productosButton.addClickListener(event -> showModule(
                productosButton,
                "Gestión de Productos",
                "Panel de Productos",
                "Inventario, precios y presentaciones",
                VaadinIcon.PACKAGE,
                new ProductoCrudView(productoService)
        ));

        serviciosButton.addClickListener(event -> showModule(
                serviciosButton,
                "Gestión de Servicios",
                "Catálogo de Servicios",
                "Tarifas de consultas, procedimientos y servicios veterinarios",
                VaadinIcon.STETHOSCOPE,
                new ServicioCrudView(productoService)
        ));

        proveedoresButton.addClickListener(event -> showModule(
                proveedoresButton,
                "Gestión de Proveedores",
                "Panel de Proveedores",
                "Información de contacto y estado comercial",
                VaadinIcon.TRUCK,
                new ProveedorView(proveedorService)
        ));

        usuariosButton.addClickListener(event -> showModule(
                usuariosButton,
                "Gestión de Usuarios",
                "Panel de Usuarios",
                "Accesos internos y credenciales del sistema",
                VaadinIcon.USERS,
                new UsuarioView(usuarioService, empleadoService, passwordEncoder)
        ));

        empleadosButton.addClickListener(event -> {
            recursosHumanosSubMenu.setVisible(true);
            showModule(
                    empleadosButton,
                    "Gestión de Empleados",
                    "Panel de Empleados",
                    "Información general del equipo de trabajo y roles",
                    VaadinIcon.GROUP,
                    new EmpleadoView(empleadoService, personaService, nominaService)
            );
            recursosHumanosButton.addClassName("menu-button-active");
        });

        clientesButton.addClickListener(event -> showModule(
                clientesButton,
                "Gestión de Clientes",
                "Panel de Clientes",
                "Expedientes, mascotas e historial comercial",
                VaadinIcon.USER,
                new ClienteView(clienteService)
        ));

        mascotasButton.addClickListener(event -> showModule(
                mascotasButton,
                "Gestión de Mascotas",
                "Panel de Mascotas",
                "Registro de pacientes y propietarios",
                VaadinIcon.HEART,
                new MascotaView(mascotaService, clienteService)
        ));

        citasButton.addClickListener(event -> showModule(
                citasButton,
                "Gestión de Citas",
                "Panel de Citas",
                "Programación de servicios veterinarios",
                VaadinIcon.CALENDAR,
                new CitaView(citaService, clienteService, empleadoService, productoService)
        ));

        ventasButton.addClickListener(event -> ventasSubmenu.setVisible(!ventasSubmenu.isVisible()));
        recursosHumanosButton.addClickListener(event ->
                recursosHumanosSubMenu.setVisible(!recursosHumanosSubMenu.isVisible()));

        almacenButton.addClickListener(e -> almacenSubmenu.setVisible(!almacenSubmenu.isVisible()));
        logisticaButton.addClickListener(e -> logisticaSubmenu.setVisible(!logisticaSubmenu.isVisible()));

        inventarioGlobalBtn.addClickListener(e -> {
            almacenSubmenu.setVisible(true);
            showModule(inventarioGlobalBtn, "Almacén e Inventario", "Inventario Global", "Consulta consolidada de existencias globales y por almacén.", VaadinIcon.GLOBE, new InventarioGlobalView(inventarioService));
            almacenButton.addClassName("menu-button-active");
        });
        gestionAlmacenesBtn.addClickListener(e -> {
            almacenSubmenu.setVisible(true);
            showModule(gestionAlmacenesBtn, "Almacén e Inventario", "Gestión de Almacenes", "Administración de ubicaciones físicas y sucursales.", VaadinIcon.BUILDING, new AlmacenView(almacenService, inventarioService));
            almacenButton.addClassName("menu-button-active");
        });
        controlLotesBtn.addClickListener(e -> {
            almacenSubmenu.setVisible(true);
            showModule(
                    controlLotesBtn,
                    "Almacén e Inventario",
                    "Control de Lotes",
                    "Seguimiento de trazabilidad y fechas de caducidad.",
                    VaadinIcon.BARCODE,
                    new LoteView(loteService, productoService)
            );
            almacenButton.addClassName("menu-button-active");
        });
        ajustesAuditoriaBtn.addClickListener(e -> {
            almacenSubmenu.setVisible(true);
            showModule(ajustesAuditoriaBtn, "Almacén e Inventario", "Ajustes de Inventario", "Auditoría, registro de mermas y sobrantes físicos.", VaadinIcon.ADJUST, new AjustesInventarioView(ajusteInventarioService, almacenService, productoService, loteService, empleadoService, securityService, inventarioService));
            almacenButton.addClassName("menu-button-active");
        });
        vehiculosBtn.addClickListener(e -> {
            logisticaSubmenu.setVisible(true);
            showModule(
                    vehiculosBtn,
                    "Logística",
                    "Flota de Vehículos",
                    "Gestión y estado de los camiones de la empresa.",
                    VaadinIcon.TRUCK,
                    new VehiculoView(vehiculoService)
            );
            logisticaButton.addClassName("menu-button-active");
        });
        rutaBtn.addClickListener(e -> {
            logisticaSubmenu.setVisible(true);
            showModule(
                    rutaBtn,
                    "Logística",
                    "Configuración de Rutas",
                    "Catálogo estándar de trayectos, distancias y viáticos sugeridos.",
                    VaadinIcon.ROAD,
                    new RutaView(rutaService)
            );
            logisticaButton.addClassName("menu-button-active");
        });

        finanzasButton.addClickListener(e -> finanzasSubmenu.setVisible(!finanzasSubmenu.isVisible()));
        gastosGeneralesBtn.addClickListener(e -> {
            finanzasSubmenu.setVisible(true);
            showModule(
                    gastosGeneralesBtn,
                    "Finanzas y Contabilidad",
                    "Gastos Operativos",
                    "Registro manual de salidas de dinero, mantenimiento, servicios e imprevistos.",
                    VaadinIcon.MONEY_WITHDRAW,
                    new GastosView(gastoOperativoService)
            );
            finanzasButton.addClassName("menu-button-active");
        });

        class NavegadorCompras {
            void mostrarHistorial() {
                ComprasView vista = new ComprasView(compraService);
                vista.setAccionNavegarRegistro(this::mostrarRegistro);

                showModule(historialComprasBtn, "Logística", "Gestión de Compras",
                        "Consulta y registro de órdenes de abastecimiento.", VaadinIcon.SHOP,
                        vista);
            }

            void mostrarRegistro(Long idBorrador) {
                RegistroCompraView vista = new RegistroCompraView(proveedorService, productoService, compraService, inventarioService);
                vista.configurarVista(idBorrador, this::mostrarHistorial);

                String sub = idBorrador == null ? "Crear nueva orden" : "Continuar borrador";
                showModule(historialComprasBtn, "Logística", "Registrar Compra",
                        sub, VaadinIcon.CART,
                        vista);
            }
        }
        NavegadorCompras navCompras = new NavegadorCompras();
        historialComprasBtn.addClickListener(e -> {
            logisticaSubmenu.setVisible(true);
            navCompras.mostrarHistorial();
            logisticaButton.addClassName("menu-button-active");
        });

        recepcionesBtn.addClickListener(e -> {
            logisticaSubmenu.setVisible(true);
            showModule(recepcionesBtn, "Logística", "Gestión de Recepciones", "Entrada física de mercancía.", VaadinIcon.INBOX, new GestionRecepcionesView(almacenService, loteService, vehiculoService, empleadoService, rutaService, recepcionService));
            logisticaButton.addClassName("menu-button-active");
        });
        despachosBtn.addClickListener(e -> {
            logisticaSubmenu.setVisible(true);
            showModule(
                    despachosBtn,
                    "Logística",
                    "Gestión de Despachos",
                    "Control de salida de mercancía en vehículos de la empresa.",
                    VaadinIcon.OUTBOX,
                    new GestionDespachosView(despachoService, vehiculoService, empleadoService, loteService, transporteService)
            );
            logisticaButton.addClassName("menu-button-active");
        });

        class NavegadorTransferencias {
            void mostrarHistorial() {
                showModule(regTransferenciaBtn, "Logística", "Gestión de Transferencias",
                        "Auditoría y seguimiento de movimientos de mercancía entre almacenes.", VaadinIcon.EXCHANGE,
                        new TransferenciasView(transferenciaService, this::mostrarRegistro));
            }

            void mostrarRegistro() {
                showModule(regTransferenciaBtn, "Logística", "Transferencias",
                        "Crear una nueva orden de movimiento interno.", VaadinIcon.EXCHANGE,
                        new RegistroTransferenciaView(almacenService, transferenciaService, inventarioService, this::mostrarHistorial));
            }
        }
        NavegadorTransferencias navTransferencias = new NavegadorTransferencias();
        regTransferenciaBtn.addClickListener(e -> {
            logisticaSubmenu.setVisible(true);
            navTransferencias.mostrarHistorial();
            logisticaButton.addClassName("menu-button-active");
        });

        registrarVentaButton.addClickListener(event -> {
            ventasSubmenu.setVisible(true);
            showModule(
                    registrarVentaButton,
                    "Gestión de Ventas",
                    "Registrar Venta",
                    "Registro de clientes, productos y descuentos de venta",
                    VaadinIcon.CART,
                    new VentaView(ventaService, clienteService, empleadoService, productoService, almacenService, loteService,
                            cuentaBancariaTransferenciaPdfService)
            );
            ventasButton.addClassName("menu-button-active");
        });

        listaVentasButton.addClickListener(event -> {
            ventasSubmenu.setVisible(true);
            showModule(
                    listaVentasButton,
                    "Gestión de Ventas",
                    "Lista de Ventas",
                    "Consulta de ventas registradas, cobros y balances pendientes",
                    VaadinIcon.LIST,
                    new ListaVentasView(ventaService, facturaVentaPdfService, cuentaBancariaTransferenciaPdfService)
            );
            ventasButton.addClassName("menu-button-active");
        });

        cobrosButton.addClickListener(event -> {
            ventasSubmenu.setVisible(true);
            showModule(
                    cobrosButton,
                    "Gestión de Cobros",
                    "Panel de Cobros",
                    "Cartera pendiente, aplicación de pagos e historial de movimientos",
                    VaadinIcon.MONEY,
                    new CobroView(ventaService, cuentaBancariaTransferenciaPdfService)
            );
            ventasButton.addClassName("menu-button-active");
        });

        class NavegadorDevoluciones {
            void mostrarHistorial() {
                DevolucionesView vistaHistorial = new DevolucionesView(
                        devolucionVentaService,
                        authContext,
                        this::mostrarRegistro
                );
                showModule(devolucionesBtn, "Gestión de Ventas", "Devoluciones",
                        "Consulta de notas de crédito y devoluciones de mercancía emitidas.", VaadinIcon.RECYCLE,
                        vistaHistorial);
            }

            void mostrarRegistro() {
                RegistroDevolucionView vistaRegistro = new RegistroDevolucionView(
                        ventaService,
                        almacenService,
                        devolucionVentaService,
                        securityService,
                        this::mostrarHistorial
                );
                showModule(devolucionesBtn, "Gestión de Ventas", "Registrar Devolución de Venta",
                        "Ingreso físico al inventario de productos devueltos por el cliente.", VaadinIcon.PLUS,
                        vistaRegistro);
            }
        }

        NavegadorDevoluciones navDevoluciones = new NavegadorDevoluciones();
        devolucionesBtn.addClickListener(e -> {
            ventasSubmenu.setVisible(true);
            navDevoluciones.mostrarHistorial();
            ventasButton.addClassName("menu-button-active");
        });

        nominaButton.addClickListener(event -> {
            recursosHumanosSubMenu.setVisible(true);
            showModule(
                nominaButton,
                "Gestión de Nómina",
                "Panel de Nómina",
                "Generación y control de nóminas del personal",
                VaadinIcon.INVOICE,
                new NominaView(corridaNominaService, detalleNominaService, configuracionNominaService, diaFeriadoService,
                        empleadoService, periodoFiscalService)
            );
            recursosHumanosButton.addClassName("menu-button-active");
        });

        vacacionesButton.addClickListener(event -> {
            recursosHumanosSubMenu.setVisible(true);
            showModule(
                    vacacionesButton,
                    "Gestión de Vacaciones",
                    "Control de Vacaciones",
                    "Programación y registro de descansos del personal",
                    VaadinIcon.FLIGHT_TAKEOFF,
                    new VacacionEmpleadoView(vacacionEmpleadoService, empleadoService, diaFeriadoService, configuracionNominaService)
            );
            recursosHumanosButton.addClassName("menu-button-active");
        });

        deduccionesButton.addClickListener(event -> {
            recursosHumanosSubMenu.setVisible(true);
            showModule(
                    deduccionesButton,
                    "Gestión de Deducciones",
                    "Panel de Deducciones",
                    "Administración de embargos, préstamos y anticipos de salario",
                    VaadinIcon.MONEY_WITHDRAW,
                    new DeduccionesView(embargoSalarialService, anticipoSalarioService, prestamoEmpleadoService, empleadoService, configuracionNominaService)
            );
            recursosHumanosButton.addClassName("menu-button-active");
        });

        VerticalLayout navigation = new VerticalLayout(
                inicioButton,
                almacenButton,
                almacenSubmenu,
                logisticaButton,
                logisticaSubmenu,
                productosButton,
                serviciosButton,
                proveedoresButton,
                usuariosButton,
                recursosHumanosButton,
                recursosHumanosSubMenu,
                clientesButton,
                mascotasButton,
                citasButton,
                ventasButton,
                ventasSubmenu,
                finanzasButton,
                finanzasSubmenu
        );
        navigation.addClassName("sidebar-nav");
        navigation.setPadding(false);
        navigation.setSpacing(false);

        Div sidebarFooter = new Div();
        sidebarFooter.addClassName("sidebar-footer");
        sidebarFooter.setText("Mockup de navegación modular");

        VerticalLayout sidebar = new VerticalLayout(brand, navigation, sidebarFooter);
        sidebar.addClassName("sidebar");
        sidebar.setPadding(false);
        sidebar.setSpacing(false);
        sidebar.setHeightFull();
        sidebar.expand(navigation);

        return sidebar;
    }

    private VerticalLayout createMainPanel(SecurityService securityService, HorizontalLayout shell) {
        moduleTitle.addClassName("module-title");

        Button collapseButton = new Button(VaadinIcon.ANGLE_LEFT.create());
        collapseButton.addClassName("collapse-button");
        collapseButton.setAriaLabel("Contraer menú");
        collapseButton.getElement().setProperty("title", "Ocultar menú lateral");
        collapseButton.addClickListener(event -> {
            boolean sidebarCollapsed = shell.getClassNames().contains("sidebar-collapsed");

            if (sidebarCollapsed) {
                shell.removeClassName("sidebar-collapsed");
                collapseButton.setIcon(VaadinIcon.ANGLE_LEFT.create());
                collapseButton.setAriaLabel("Contraer menú");
                collapseButton.getElement().setProperty("title", "Ocultar menú lateral");
            } else {
                shell.addClassName("sidebar-collapsed");
                collapseButton.setIcon(VaadinIcon.ANGLE_RIGHT.create());
                collapseButton.setAriaLabel("Expandir menú");
                collapseButton.getElement().setProperty("title", "Mostrar menú lateral");
            }
        });

        Button avatarButton = new Button();
        avatarButton.addClassName("user-avatar-button");
        avatarButton.setAriaLabel("Menú de usuario");
        actualizarAvatarUsuario(avatarButton, securityService);

        ContextMenu userMenu = new ContextMenu(avatarButton);
        userMenu.setOpenOnClick(true);
        userMenu.addItem("Configuración", event -> showModule(
                null,
                "Configuración",
                "Configuración de Usuario",
                "Perfil, foto de usuario y contraseña",
                VaadinIcon.COG,
                new ConfiguracionUsuarioView(securityService, () -> actualizarAvatarUsuario(avatarButton, securityService))
        ));
        userMenu.addItem("Salir", event -> authContext.logout());

        HorizontalLayout topBarLeft = new HorizontalLayout(collapseButton, moduleTitle);
        topBarLeft.addClassName("topbar-left");
        topBarLeft.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout topBarRight = new HorizontalLayout(avatarButton);
        topBarRight.setAlignItems(FlexComponent.Alignment.CENTER);
        topBarRight.setSpacing(true);

        HorizontalLayout moduleHeader = new HorizontalLayout(topBarLeft, topBarRight);
        moduleHeader.addClassName("module-header");
        moduleHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        moduleHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        moduleHeader.setPadding(false);
        moduleHeader.setSpacing(false);

        contentArea.addClassName("content-area");

        VerticalLayout mainPanel = new VerticalLayout(moduleHeader, contentArea);
        mainPanel.addClassName("main-panel");
        mainPanel.setSizeFull();
        mainPanel.setPadding(false);
        mainPanel.setSpacing(false);
        mainPanel.expand(contentArea);
        mainPanel.getStyle().setFlexGrow("1");

        return mainPanel;
    }

    private Button createMenuButton(VaadinIcon icon, String label) {
        Button button = new Button(label, icon.create());
        button.addClassName("menu-button");
        button.setWidthFull();
        menuButtons.add(button);
        return button;
    }

    private Button createSubmenuButton(VaadinIcon icon, String label) {
        Button button = new Button(label, icon.create());
        button.addClassName("submenu-button");
        button.setWidthFull();
        menuButtons.add(button);
        return button;
    }

    private void showModule(
            Button activeButton,
            String title,
            String panelTitle,
            String panelDescription,
            VaadinIcon panelIcon,
            Component moduleView
    ) {
        menuButtons.forEach(button -> button.removeClassName("menu-button-active"));
        if (activeButton != null) {
            activeButton.addClassName("menu-button-active");
        }

        moduleTitle.setText(title);

        contentArea.removeAll();
        contentArea.add(createPanelSummary(panelTitle, panelDescription, panelIcon), createModuleContent(moduleView));
    }

    private Component createPanelSummary(String title, String description, VaadinIcon icon) {
        Div iconWrapper = new Div();
        iconWrapper.addClassName("panel-summary-icon");
        iconWrapper.add(icon.create());

        H3 heading = new H3(title);
        heading.addClassName("panel-summary-title");

        Paragraph copy = new Paragraph(description);
        copy.addClassName("panel-summary-description");

        VerticalLayout text = new VerticalLayout(heading, copy);
        text.setPadding(false);
        text.setSpacing(false);

        HorizontalLayout summary = new HorizontalLayout(iconWrapper, text);
        summary.addClassName("panel-summary");
        summary.setAlignItems(FlexComponent.Alignment.CENTER);

        return summary;
    }

    private Component createModuleContent(Component moduleView) {
        Div moduleCard = new Div(moduleView);
        moduleCard.addClassName("module-card");

        HorizontalLayout layout = new HorizontalLayout(moduleCard);
        layout.addClassName("module-content-grid");
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        layout.setPadding(false);
        layout.setSpacing(false);

        return layout;
    }

    private void actualizarAvatarUsuario(Button avatarButton, SecurityService securityService) {
        avatarButton.setText("");
        avatarButton.setIcon(null);

        Usuario usuario = securityService.obtenerUsuarioAutenticado();
        if (usuario != null && usuario.getFotoPerfil() != null && usuario.getFotoPerfil().length > 0) {
            Image foto = new Image("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(usuario.getFotoPerfil()), "Foto de perfil");
            foto.addClassName("user-avatar-image");
            avatarButton.setIcon(foto);
            return;
        }

        Empleado empleado = securityService.obtenerEmpleadoAutenticado();
        if (empleado != null && empleado.getPersona() != null) {
            avatarButton.setText(obtenerIniciales(empleado.getPersona()));
            return;
        }

        avatarButton.setIcon(VaadinIcon.USER.create());
    }

    private String obtenerIniciales(Persona persona) {
        String nombre = persona.getNombre() == null ? "" : persona.getNombre().trim();
        String apellido = persona.getApellido() == null ? "" : persona.getApellido().trim();
        String primera = nombre.isBlank() ? "" : nombre.substring(0, 1);
        String segunda = apellido.isBlank() ? "" : apellido.substring(0, 1);
        String resultado = (primera + segunda).toUpperCase();
        return resultado.isBlank() ? "U" : resultado;
    }

}
