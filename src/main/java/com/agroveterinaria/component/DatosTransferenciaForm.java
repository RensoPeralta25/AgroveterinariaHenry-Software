package com.agroveterinaria.component;

import com.agroveterinaria.service.VentaService;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadEvent;

import java.io.ByteArrayOutputStream;

public class DatosTransferenciaForm extends VerticalLayout {

    private final TextField bancoOrigen = new TextField("Banco de origen");
    private final TextField titular = new TextField("Titular de la cuenta");
    private final TextField referencia = new TextField("Referencia bancaria");
    private final Upload comprobante = new Upload();
    private final Checkbox confirmada = new Checkbox(
            "Confirmo que revisé el comprobante, el monto y la cuenta receptora"
    );

    private byte[] comprobanteBytes;
    private String nombreComprobante;
    private String tipoContenido;

    public DatosTransferenciaForm() {
        setPadding(false);
        setSpacing(true);
        setWidthFull();

        bancoOrigen.setWidthFull();
        bancoOrigen.setMaxLength(80);
        titular.setWidthFull();
        titular.setMaxLength(150);
        referencia.setWidthFull();
        referencia.setMaxLength(100);

        comprobante.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp", "application/pdf");
        comprobante.setMaxFileSize(5 * 1024 * 1024);
        comprobante.setMaxFiles(1);
        comprobante.setDropLabel(new Span("Arrastra el comprobante aquí"));
        comprobante.setUploadHandler(this::procesarComprobante);
        comprobante.getElement().addEventListener("file-remove", event -> limpiarComprobante());

        add(bancoOrigen, titular, referencia, comprobante, confirmada);
        setVisible(false);
    }

    public VentaService.DatosTransferencia obtenerDatos() {
        return new VentaService.DatosTransferencia(
                bancoOrigen.getValue(),
                titular.getValue(),
                referencia.getValue(),
                comprobanteBytes,
                nombreComprobante,
                tipoContenido,
                confirmada.getValue()
        );
    }

    public void sugerirTitular(String nombreTitular) {
        if (titular.isEmpty() && nombreTitular != null) {
            titular.setValue(nombreTitular);
        }
    }

    public void limpiar() {
        bancoOrigen.clear();
        titular.clear();
        referencia.clear();
        confirmada.setValue(false);
        comprobante.clearFileList();
        limpiarComprobante();
    }

    private void procesarComprobante(UploadEvent event) {
        try (var stream = event.getInputStream();
             var output = new ByteArrayOutputStream()) {
            stream.transferTo(output);
            byte[] bytes = output.toByteArray();
            getUI().ifPresent(ui -> ui.access(() -> {
                comprobanteBytes = bytes;
                nombreComprobante = event.getFileName();
                tipoContenido = event.getContentType();
            }));
        } catch (Exception exception) {
            getUI().ifPresent(ui -> ui.access(this::limpiarComprobante));
        }
    }

    private void limpiarComprobante() {
        comprobanteBytes = null;
        nombreComprobante = null;
        tipoContenido = null;
    }
}
