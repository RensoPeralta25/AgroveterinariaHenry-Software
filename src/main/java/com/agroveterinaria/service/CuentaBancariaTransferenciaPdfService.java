package com.agroveterinaria.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class CuentaBancariaTransferenciaPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
    private static final String BENEFICIARIO = "Henrry Esteban Arias Alvarez";
    private static final String BANCO = "BanReservas";
    private static final String NUMERO_CUENTA = "251-000033-5";
    private static final String TIPO_CUENTA = "Corriente";

    public byte[] generarCuentaBancariaPdf() {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(crearHtml(), baseUri());
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el PDF de transferencia bancaria.", exception);
        }
    }

    private String crearHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <style>
                    @page {
                      size: A4;
                      margin: 20mm;
                    }

                    body {
                      font-family: Arial, sans-serif;
                      color: #111827;
                      font-size: 13px;
                      line-height: 1.5;
                    }

                    h1, h2, p {
                      margin: 0;
                    }

                    .header {
                      border-bottom: 2px solid #111827;
                      padding-bottom: 16px;
                      margin-bottom: 24px;
                    }

                    .brand {
                      font-size: 22px;
                      letter-spacing: 0;
                      margin-bottom: 4px;
                    }

                    .subtitle {
                      color: #4b5563;
                    }

                    .panel {
                      border: 1px solid #d1d5db;
                      padding: 18px;
                      margin-bottom: 18px;
                    }

                    .row {
                      display: table;
                      width: 100%%;
                      border-bottom: 1px solid #e5e7eb;
                      padding: 10px 0;
                    }

                    .row:last-child {
                      border-bottom: none;
                    }

                    .label, .value {
                      display: table-cell;
                      vertical-align: top;
                    }

                    .label {
                      width: 35%%;
                      color: #6b7280;
                      font-size: 11px;
                      text-transform: uppercase;
                    }

                    .value {
                      font-size: 16px;
                      font-weight: bold;
                    }

                    .note {
                      background: #f3f4f6;
                      border-left: 4px solid #111827;
                      padding: 12px 14px;
                      color: #374151;
                    }

                    .footer {
                      margin-top: 30px;
                      color: #6b7280;
                      font-size: 10px;
                      border-top: 1px solid #e5e7eb;
                      padding-top: 10px;
                    }
                  </style>
                </head>
                <body>
                  <div class="header">
                    <h1 class="brand">Agroveterinaria Henry</h1>
                    <p class="subtitle">Informacion para transferencia bancaria</p>
                  </div>

                  <div class="panel">
                    <div class="row">
                      <div class="label">Beneficiario</div>
                      <div class="value">%s</div>
                    </div>
                    <div class="row">
                      <div class="label">Banco</div>
                      <div class="value">%s</div>
                    </div>
                    <div class="row">
                      <div class="label">Numero de cuenta</div>
                      <div class="value">%s</div>
                    </div>
                    <div class="row">
                      <div class="label">Tipo de cuenta</div>
                      <div class="value">%s</div>
                    </div>
                  </div>

                  <p class="note">Favor confirmar la transferencia con el personal de caja antes de cerrar el pago en el sistema.</p>

                  <p class="footer">Documento generado automaticamente desde el sistema administrativo el %s.</p>
                </body>
                </html>
                """.formatted(
                escape(BENEFICIARIO),
                escape(BANCO),
                escape(NUMERO_CUENTA),
                escape(TIPO_CUENTA),
                LocalDateTime.now().format(DATE_TIME_FORMAT)
        );
    }

    private String baseUri() {
        return Optional.ofNullable(getClass().getResource("/"))
                .map(resource -> resource.toExternalForm())
                .orElse("");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
