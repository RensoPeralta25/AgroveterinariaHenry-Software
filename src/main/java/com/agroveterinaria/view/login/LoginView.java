package com.agroveterinaria.view.login;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Iniciar sesión | Agroveterinaria Henry")
@AnonymousAllowed
public class LoginView extends Div implements BeforeEnterObserver {

    private static final String LOGO_PATH = "/images/agroveterinaria-henry-logo.png";

    private final LoginForm login = new LoginForm();

    public LoginView() {
        addClassName("login-view");
        configurarFormulario();

        Div shell = new Div(crearPanelVisual(), crearPanelFormulario());
        shell.addClassName("login-shell");
        add(shell);
    }

    private Div crearPanelVisual() {
        Image logo = new Image(LOGO_PATH, "Logo de Agroveterinaria Henry");
        logo.addClassName("login-hero-logo");

        Div logoFrame = new Div(logo);
        logoFrame.addClassName("login-logo-frame");

        Span eyebrow = new Span("Cuidado, control y confianza");
        eyebrow.addClassName("login-hero-eyebrow");

        H1 title = new H1("Agroveterinaria Henry");
        title.addClassName("login-hero-title");

        Paragraph description = new Paragraph(
                "Gestiona las operaciones de la veterinaria desde un espacio seguro, claro y centralizado."
        );
        description.addClassName("login-hero-description");

        Div content = new Div(logoFrame, eyebrow, title, description);
        content.addClassName("login-hero-content");

        Div panel = new Div(content);
        panel.addClassName("login-visual-panel");
        return panel;
    }

    private Div crearPanelFormulario() {
        Image compactLogo = new Image(LOGO_PATH, "Agroveterinaria Henry");
        compactLogo.addClassName("login-form-logo");

        Span eyebrow = new Span("Panel administrativo");
        eyebrow.addClassName("login-form-eyebrow");

        H2 title = new H2("Bienvenido de nuevo");
        title.addClassName("login-form-title");

        Paragraph description = new Paragraph("Ingresa tus credenciales para continuar.");
        description.addClassName("login-form-description");

        Span securityNote = new Span("Acceso exclusivo para personal autorizado");
        securityNote.addClassName("login-security-note");

        Div content = new Div(compactLogo, eyebrow, title, description, login, securityNote);
        content.addClassName("login-form-content");

        Div panel = new Div(content);
        panel.addClassName("login-form-panel");
        return panel;
    }

    private void configurarFormulario() {
        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle("Acceso al sistema");
        i18n.getForm().setUsername("Usuario");
        i18n.getForm().setPassword("Contraseña");
        i18n.getForm().setSubmit("Iniciar sesión");
        i18n.getForm().setForgotPassword("¿Olvidaste tu contraseña?");
        i18n.getErrorMessage().setTitle("No se pudo iniciar sesión");
        i18n.getErrorMessage().setMessage("Verifica el usuario y la contraseña e intenta nuevamente.");

        login.setI18n(i18n);
        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);
        login.addClassName("login-form");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }
}
