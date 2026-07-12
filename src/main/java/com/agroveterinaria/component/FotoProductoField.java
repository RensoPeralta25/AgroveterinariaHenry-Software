package com.agroveterinaria.component;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadEvent;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class FotoProductoField extends CustomField<byte[]> {

    private byte[] fotoBytes = null;
    private final Upload uploadArchivo = new Upload();
    private final Upload uploadCamara  = new Upload();
    private final Image preview        = new Image();
    private final Button btnEliminar   = new Button("Eliminar foto");

    public FotoProductoField() {
        configurarUpload();
        construirLayout();
    }

    private void configurarUpload() {
        // Subir desde archivo
        uploadArchivo.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp");
        uploadArchivo.setMaxFileSize(25 * 1024 * 1024);
        uploadArchivo.setUploadButton(new Button("Subir archivo"));
        uploadArchivo.setDropLabel(new Span("o arrastra una imagen aquí"));
        uploadArchivo.setMaxFiles(1);
        uploadArchivo.getElement().addEventListener("file-remove", event -> limpiarFoto());
        uploadArchivo.setUploadHandler((UploadEvent event) -> procesarArchivo(event, uploadCamara));

        // Subir desde cámara
        uploadCamara.setAcceptedFileTypes("image/jpeg", "image/png");
        uploadCamara.setMaxFileSize(25 * 1024 * 1024);
        uploadCamara.setUploadButton(new Button("Tomar foto"));
        uploadCamara.setDropAllowed(false);
        uploadCamara.setMaxFiles(1);
        uploadCamara.getElement().addEventListener("file-remove", event -> limpiarFoto());
        uploadCamara.getElement().getChild(0).setAttribute("capture", "environment");
        uploadCamara.setUploadHandler((UploadEvent event) -> procesarArchivo(event, uploadArchivo));

        // Preview
        preview.setWidth("200px");
        preview.setHeight("200px");
        preview.getStyle()
                .set("object-fit", "cover")
                .set("border-radius", "8px")
                .set("display", "none");

        btnEliminar.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        btnEliminar.setVisible(false);
        btnEliminar.addClickListener(e -> limpiarFoto());
    }

    private void procesarArchivo(UploadEvent event, Upload uploadContrario) {
        try (java.io.InputStream stream = event.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesLeidos;
            while ((bytesLeidos = stream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesLeidos);
            }

            byte[] bytesFinales = baos.toByteArray();

            getUI().ifPresent(ui -> ui.access(() -> {
                uploadContrario.clearFileList();
                this.fotoBytes = bytesFinales;
                mostrarPreview();
                updateValue();
            }));

        } catch (Exception e) {
            getUI().ifPresent(ui -> ui.access(() -> {
                Notification.show("Error al procesar la imagen").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }));
        }
    }

    private void construirLayout() {
        Div seccionFoto = new Div();
        seccionFoto.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "16px")
                .set("padding", "12px")
                .set("border", "1px dashed #ccc")
                .set("border-radius", "8px")
                .set("margin-top", "8px");

        H2 titulo = new H2("Foto");
        titulo.getStyle().set("margin", "0").set("font-size", "1.2em");

        Div contenidoFoto = new Div();
        contenidoFoto.getStyle()
                .set("display", "flex")
                .set("gap", "16px")
                .set("align-items", "center")
                .set("flex-wrap", "wrap");

        Div opcionesFoto = new Div(uploadArchivo, uploadCamara);
        opcionesFoto.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "8px");

        Div contenedorPreview = new Div(preview, btnEliminar);
        contenedorPreview.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("gap", "8px");

        contenidoFoto.add(opcionesFoto, contenedorPreview);
        seccionFoto.add(titulo, contenidoFoto);

        add(seccionFoto);
    }

    private void mostrarPreview() {
        if (fotoBytes != null) {
            String base64 = Base64.getEncoder().encodeToString(fotoBytes);
            preview.getElement().setAttribute("src", "data:image/jpeg;base64," + base64);
            preview.getStyle().set("display", "block");

            btnEliminar.setVisible(!isReadOnly());
        }
    }

    private void limpiarFoto() {
        uploadArchivo.clearFileList();
        uploadCamara.clearFileList();
        this.fotoBytes = null;
        preview.getElement().removeAttribute("src");
        preview.getStyle().set("display", "none");

        btnEliminar.setVisible(false);

        updateValue();
    }

    @Override
    protected byte[] generateModelValue() {
        return this.fotoBytes;
    }

    @Override
    protected void setPresentationValue(byte[] bytes) {
        if (bytes != null && bytes.length > 0) {
            this.fotoBytes = bytes;
            mostrarPreview();
        } else {
            limpiarFoto();
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);

        uploadArchivo.setVisible(!readOnly);
        uploadCamara.setVisible(!readOnly);

        if (readOnly) {
            btnEliminar.setVisible(false);
        } else {
            btnEliminar.setVisible(fotoBytes != null);
        }
    }
}