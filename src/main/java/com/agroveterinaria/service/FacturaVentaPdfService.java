package com.agroveterinaria.service;

import com.agroveterinaria.dto.venta.FacturaVentaPdfDTO;
import com.agroveterinaria.dto.venta.LineaFacturaVentaPdfDTO;
import com.agroveterinaria.entity.Venta;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
public class FacturaVentaPdfService {

    private static final Locale LOCALE_DO = Locale.of("es", "DO");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(LOCALE_DO);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    private final VentaService ventaService;
    private final FacturaVentaPdfMapper mapper;

    public FacturaVentaPdfService(VentaService ventaService, FacturaVentaPdfMapper mapper) {
        this.ventaService = ventaService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public byte[] generarFacturaVentaPdf(Long idVenta) {
        Venta venta = ventaService.buscarPorId(idVenta)
                .orElseThrow(() -> new IllegalArgumentException("La venta seleccionada no existe."));

        BigDecimal montoCobrado = ventaService.calcularTotalCobrado(venta);
        BigDecimal balancePendiente = ventaService.calcularDeudaRestante(venta);
        FacturaVentaPdfDTO factura = mapper.toDto(venta, montoCobrado, balancePendiente);

        return renderizar(factura);
    }

    byte[] renderizar(FacturaVentaPdfDTO factura) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(crearHtml(factura), baseUri());
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar la factura PDF.", exception);
        }
    }

    String crearHtml(FacturaVentaPdfDTO factura) {
        StringBuilder lineas = new StringBuilder();
        for (LineaFacturaVentaPdfDTO linea : factura.lineas()) {
            lineas.append("""
                    <tr>
                      <td>
                        <span class="item-name">%s</span>
                        <span class="item-detail">%s</span>
                      </td>
                      <td class="num">%s</td>
                      <td class="num">%s</td>
                      <td class="num">%s</td>
                      <td class="num">%s</td>
                    </tr>
                    """.formatted(
                    escape(linea.productoNombre()),
                    escape(linea.desglosePrecios()),
                    escape(linea.cantidad()),
                    formatMoney(linea.precioUnitario()),
                    formatMoney(linea.impuesto()),
                    formatMoney(linea.subtotal())
            ));
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <style>
                    @page {
                      size: A4;
                      margin: 18mm;
                    }

                    body {
                      font-family: Arial, sans-serif;
                      color: #111827;
                      font-size: 12px;
                      line-height: 1.4;
                    }

                    h1, h2, p {
                      margin: 0;
                    }

                    .header {
                      display: table;
                      width: 100%%;
                      border-bottom: 2px solid #111827;
                      padding-bottom: 14px;
                      margin-bottom: 18px;
                    }

                    .brand, .invoice-meta {
                      display: table-cell;
                      vertical-align: top;
                    }

                    .brand h1 {
                      font-size: 22px;
                      letter-spacing: 0;
                    }

                    .brand p {
                      color: #4b5563;
                      margin-top: 4px;
                    }

                    .invoice-meta {
                      text-align: right;
                    }

                    .invoice-meta h2 {
                      font-size: 18px;
                      margin-bottom: 6px;
                    }

                    .section {
                      margin-bottom: 18px;
                    }

                    .two-columns {
                      display: table;
                      width: 100%%;
                    }

                    .column {
                      display: table-cell;
                      width: 50%%;
                      vertical-align: top;
                    }

                    .label {
                      color: #6b7280;
                      font-size: 10px;
                      text-transform: uppercase;
                    }

                    .value {
                      margin-bottom: 7px;
                    }

                    table {
                      width: 100%%;
                      border-collapse: collapse;
                    }

                    th {
                      background: #f3f4f6;
                      border-bottom: 1px solid #d1d5db;
                      padding: 8px 6px;
                      text-align: left;
                    }

                    td {
                      border-bottom: 1px solid #e5e7eb;
                      padding: 7px 6px;
                      vertical-align: top;
                    }

                    .item-name {
                      display: block;
                      font-weight: bold;
                    }

                    .item-detail {
                      display: block;
                      color: #4b5563;
                      font-size: 10px;
                      margin-top: 2px;
                    }

                    .num {
                      text-align: right;
                      white-space: nowrap;
                    }

                    .totals {
                      margin-left: auto;
                      width: 270px;
                      margin-top: 18px;
                    }

                    .totals td {
                      border-bottom: none;
                      padding: 5px 0;
                    }

                    .total-row td {
                      border-top: 2px solid #111827;
                      font-weight: bold;
                      font-size: 14px;
                      padding-top: 9px;
                    }

                    .footer {
                      margin-top: 28px;
                      color: #6b7280;
                      font-size: 10px;
                      border-top: 1px solid #e5e7eb;
                      padding-top: 10px;
                    }

                    .responsibles {
                      display: table;
                      width: 100%%;
                      margin-top: 36px;
                      page-break-inside: avoid;
                    }

                    .responsible {
                      display: table-cell;
                      width: 46%%;
                      text-align: center;
                      padding: 10px 8px;
                      background: #f9fafb;
                      border: 1px solid #e5e7eb;
                    }

                    .responsible-label {
                      display: block;
                      color: #6b7280;
                      font-size: 10px;
                      text-transform: uppercase;
                      margin-bottom: 4px;
                    }

                    .responsible-name {
                      display: block;
                      color: #111827;
                      font-size: 12px;
                      font-weight: bold;
                    }

                    .responsible-spacer {
                      display: table-cell;
                      width: 8%%;
                    }
                  </style>
                </head>
                <body>
                  <div class="header">
                    <div class="brand">
                      <h1>Agroveterinaria Henry</h1>
                      <p>Factura de venta</p>
                    </div>
                    <div class="invoice-meta">
                      <h2>Venta #%s</h2>
                      <p>%s</p>
                      <p>%s</p>
                    </div>
                  </div>

                  <div class="section two-columns">
                    <div class="column">
                      <p class="label">Cliente</p>
                      <p class="value">%s</p>
                      <p class="label">Cedula</p>
                      <p class="value">%s</p>
                      <p class="label">Telefono</p>
                      <p class="value">%s</p>
                      <p class="label">Direccion</p>
                      <p class="value">%s</p>
                    </div>
                    <div class="column">
                      <p class="label">Vendedor</p>
                      <p class="value">%s</p>
                      <p class="label">Comprobante fiscal</p>
                      <p class="value">%s</p>
                      <p class="label">Estado</p>
                      <p class="value">%s</p>
                      <p class="label">Despacho</p>
                      <p class="value">%s</p>
                      <p class="label">Condiciones de crédito</p>
                      <p class="value">%s</p>
                    </div>
                  </div>

                  <table>
                    <thead>
                      <tr>
                        <th>Producto</th>
                        <th class="num">Cantidad</th>
                        <th class="num">Precio</th>
                        <th class="num">Impuesto</th>
                        <th class="num">Total línea</th>
                      </tr>
                    </thead>
                    <tbody>
                      %s
                    </tbody>
                  </table>

                  <table class="totals">
                    <tr>
                      <td>Subtotal</td>
                      <td class="num">%s</td>
                    </tr>
                    <tr>
                      <td>Impuestos</td>
                      <td class="num">%s</td>
                    </tr>
                    <tr>
                      <td>Descuento</td>
                      <td class="num">-%s</td>
                    </tr>
                    <tr>
                      <td>Costo de envío</td>
                      <td class="num">%s</td>
                    </tr>
                    <tr>
                      <td>Cobrado</td>
                      <td class="num">%s</td>
                    </tr>
                    <tr>
                      <td>Balance pendiente</td>
                      <td class="num">%s</td>
                    </tr>
                    <tr class="total-row">
                      <td>Total</td>
                      <td class="num">%s</td>
                    </tr>
                  </table>

                  <div class="responsibles">
                    <div class="responsible">
                      <span class="responsible-label">Despachado por</span>
                      <span class="responsible-name">%s</span>
                    </div>
                    <div class="responsible-spacer"></div>
                    <div class="responsible">
                      <span class="responsible-label">Recibido por</span>
                      <span class="responsible-name">%s</span>
                    </div>
                  </div>

                  <p class="footer">Documento generado automáticamente desde el sistema administrativo.</p>
                </body>
                </html>
                """.formatted(
                factura.idVenta(),
                formatDate(factura.fechaHoraVenta()),
                escape(factura.estado()),
                escape(factura.clienteNombre()),
                escape(factura.clienteCedula()),
                escape(factura.clienteTelefono()),
                escape(factura.clienteDireccion()),
                escape(factura.vendedorNombre()),
                escape(factura.comprobanteFiscal()),
                escape(factura.estado()),
                factura.llevaDespacho() ? "Si" : "No",
                escape(factura.condicionesCredito()),
                lineas,
                formatMoney(factura.subtotal()),
                formatMoney(factura.impuestos()),
                formatMoney(factura.descuento()),
                formatMoney(factura.costoEnvio()),
                formatMoney(factura.montoCobrado()),
                formatMoney(factura.balancePendiente()),
                formatMoney(factura.montoTotal()),
                escape(factura.vendedorNombre()),
                escape(factura.clienteNombre())
        );
    }

    private String baseUri() {
        return Optional.ofNullable(getClass().getResource("/"))
                .map(resource -> resource.toExternalForm())
                .orElse("");
    }

    private String formatDate(java.time.LocalDateTime value) {
        return value != null ? value.format(DATE_TIME_FORMAT) : "-";
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value != null ? value : BigDecimal.ZERO);
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
