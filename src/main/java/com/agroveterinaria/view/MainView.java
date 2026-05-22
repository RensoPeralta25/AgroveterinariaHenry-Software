package com.agroveterinaria.view;

import com.agroveterinaria.service.ProductoService;
import com.agroveterinaria.service.ProveedorService;
import com.agroveterinaria.service.UsuarioService;
import com.agroveterinaria.view.producto.ProductoCrudView;
import com.agroveterinaria.view.proveedor.ProveedorView;
import com.agroveterinaria.view.usuario.UsuarioView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route("")
@PageTitle("Agroveterinaria Henry | Panel principal")
public class MainView extends Div {

    private final Div contentArea = new Div();
    private final H2 moduleTitle = new H2();
    private final List<Button> menuButtons = new ArrayList<>();

    public MainView(
            UsuarioService usuarioService,
            ProductoService productoService,
            ProveedorService proveedorService
    ) {
        addClassName("main-view");
        setSizeFull();

        VerticalLayout sidebar = createSidebar(usuarioService, productoService, proveedorService);
        VerticalLayout mainPanel = createMainPanel();

        HorizontalLayout shell = new HorizontalLayout(sidebar, mainPanel);
        shell.addClassName("app-shell");
        shell.setSizeFull();
        shell.setSpacing(false);
        shell.setPadding(false);
        shell.expand(mainPanel);

        add(shell);
        showModule(
                menuButtons.get(0),
                "Gestión de Productos",
                "Panel de Productos",
                "Inventario, precios y presentaciones",
                VaadinIcon.PACKAGE,
                new ProductoCrudView(productoService)
        );
    }

    private VerticalLayout createSidebar(
            UsuarioService usuarioService,
            ProductoService productoService,
            ProveedorService proveedorService
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

        Button inicioButton = createMenuButton(VaadinIcon.HOME, "Inicio");
        Button productosButton = createMenuButton(VaadinIcon.PACKAGE, "Productos");
        Button proveedoresButton = createMenuButton(VaadinIcon.TRUCK, "Proveedores");
        Button usuariosButton = createMenuButton(VaadinIcon.USERS, "Usuarios");

        inicioButton.addClickListener(event -> showModule(
                inicioButton,
                "Inicio",
                "Panel Principal",
                "Resumen general de la operación administrativa.",
                VaadinIcon.DASHBOARD,
                createWelcomePanel()
        ));

        productosButton.addClickListener(event -> showModule(
                productosButton,
                "Gestión de Productos",
                "Panel de Productos",
                "Inventario, precios y presentaciones",
                VaadinIcon.PACKAGE,
                new ProductoCrudView(productoService)
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
                new UsuarioView(usuarioService)
        ));

        VerticalLayout navigation = new VerticalLayout(inicioButton, productosButton, proveedoresButton, usuariosButton);
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

    private VerticalLayout createMainPanel() {
        moduleTitle.addClassName("module-title");

        Button collapseButton = new Button(VaadinIcon.ANGLE_LEFT.create());
        collapseButton.addClassName("collapse-button");
        collapseButton.setAriaLabel("Contraer menú");

        Div avatar = new Div();
        avatar.addClassName("user-avatar");
        avatar.add(VaadinIcon.USER.create());

        HorizontalLayout topBarLeft = new HorizontalLayout(collapseButton, moduleTitle);
        topBarLeft.addClassName("topbar-left");
        topBarLeft.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout moduleHeader = new HorizontalLayout(topBarLeft, avatar);
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

        return mainPanel;
    }

    private Button createMenuButton(VaadinIcon icon, String label) {
        Button button = new Button(label, icon.create());
        button.addClassName("menu-button");
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
        activeButton.addClassName("menu-button-active");

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

        VerticalLayout quickActions = createQuickActions();

        HorizontalLayout layout = new HorizontalLayout(moduleCard, quickActions);
        layout.addClassName("module-content-grid");
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        layout.setPadding(false);
        layout.setSpacing(false);

        return layout;
    }

    private VerticalLayout createQuickActions() {
        H3 title = new H3("Acciones rápidas");
        title.addClassName("quick-actions-title");

        Button registrarProveedor = createQuickActionButton(VaadinIcon.TRUCK, "Registrar proveedor");
        Button crearUsuario = createQuickActionButton(VaadinIcon.USER_CARD, "Crear usuario");
        Button inventarioBajo = createQuickActionButton(VaadinIcon.BAR_CHART, "Ver inventario bajo");

        VerticalLayout quickActions = new VerticalLayout(title, registrarProveedor, crearUsuario, inventarioBajo);
        quickActions.addClassName("quick-actions-card");
        quickActions.setPadding(false);
        quickActions.setSpacing(false);

        return quickActions;
    }

    private Button createQuickActionButton(VaadinIcon icon, String label) {
        Button button = new Button(label, icon.create());
        button.addClassName("quick-action-button");
        button.setWidthFull();
        return button;
    }

    private Component createWelcomePanel() {
        Div placeholder = new Div();
        placeholder.addClassName("welcome-placeholder");
        placeholder.setText("Selecciona un módulo para trabajar con productos, proveedores o usuarios.");
        return placeholder;
    }
}
