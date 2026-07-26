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
public class FacturaVentaTermicaPdfService {

    private static final Locale LOCALE_DO = Locale.of("es", "DO");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(LOCALE_DO);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    private final VentaService ventaService;
    private final FacturaVentaPdfMapper mapper;

    public FacturaVentaTermicaPdfService(VentaService ventaService, FacturaVentaPdfMapper mapper) {
        this.ventaService = ventaService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public byte[] generarFacturaTermicaPdf(Long idVenta) {
        Venta venta = ventaService.buscarPorId(idVenta)
                .orElseThrow(() -> new IllegalArgumentException("La venta seleccionada no existe."));

        BigDecimal montoCobrado = ventaService.calcularTotalCobrado(venta);
        BigDecimal balancePendiente = ventaService.calcularDeudaRestante(venta);

        FacturaVentaPdfDTO factura = mapper.toDto(venta, montoCobrado, balancePendiente);

        return renderizar(factura);
    }

    private byte[] renderizar(FacturaVentaPdfDTO factura) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(crearHtml(factura), baseUri());
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new IllegalStateException("No se pudo generar el ticket térmico PDF.", exception);
        }
    }

    private String crearHtml(FacturaVentaPdfDTO factura) {
        StringBuilder lineas = new StringBuilder();
        for (LineaFacturaVentaPdfDTO linea : factura.lineas()) {
            lineas.append("""
                    <tr>
                      <td class="col-prod">
                        <span class="item-name">%s</span>
                        <span class="item-detail">%s x %s (Imp: %s)</span>
                      </td>
                      <td class="col-total num">%s</td>
                    </tr>
                    """.formatted(
                    escape(linea.productoNombre()),
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
                        size: 58mm 300mm;
                        margin: 2mm;
                      }
    
                      body {
                        width: 54mm;\s
                        font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                        color: #000;
                        font-size: 11px;
                        line-height: 1.2;
                        margin: 0;
                        padding: 0;
                      }
    
                      .text-center { text-align: center; }
                      .bold { font-weight: bold; }
                      .num { text-align: right; }
    
                      h1 { font-size: 15px; margin: 0 0 2px 0; text-transform: uppercase;}
                      p { margin: 1px 0; }
    
                      .divider {\s
                          border-bottom: 1px dashed #000;\s
                          margin: 5px 0;\s
                      }
    
                      .info-grid {
                          width: 100%%;
                          margin-bottom: 5px;
                      }
                      .info-grid td { padding: 1px 0; font-size: 10px;}
    
                      table.items {
                        width: 100%%;
                        border-collapse: collapse;
                        margin-top: 5px;
                      }
    
                      table.items th {
                        border-bottom: 1px solid #000;
                        padding-bottom: 2px;
                        text-align: left;
                        font-size: 11px;
                      }
    
                      table.items td {
                        padding: 4px 0;
                        vertical-align: top;
                        border-bottom: 1px dotted #ccc;
                      }
    
                      .item-name { display: block; font-weight: bold; font-size: 12px; margin-bottom: 1px;}
                      .item-detail { display: block; font-size: 10px; color: #333; }
                      .col-total { font-weight: bold; vertical-align: middle !important; font-size: 11px; }
    
                      .totals {
                        width: 100%%;
                        margin-top: 5px;
                        border-collapse: collapse;
                      }
    
                      .totals td {
                        padding: 2px 0;
                        font-size: 11px;
                      }
    
                      .total-row td {
                        border-top: 1px dashed #000;
                        font-weight: bold;
                        font-size: 14px;
                        padding-top: 4px;
                      }
    
                      .footer {
                        text-align: center;
                        margin-top: 10px;
                        font-size: 10px;
                      }
                    </style>
                </head>
                <body>
                  <div class="text-center">
                    <h1>Agroveterinaria Henry</h1>
                    <p>Factura de venta</p>
                  </div>
                  
                  <div class="divider"></div>

                  <table class="info-grid">
                    <tr><td><span class="bold">Venta #:</span> %s</td><td class="num">%s</td></tr>
                    <tr><td><span class="bold">Cliente:</span> %s</td><td class="num">%s</td></tr>
                    <tr><td><span class="bold">Cédula:</span> %s</td><td class="num">%s</td></tr>
                    <tr><td><span class="bold">Vendedor:</span> %s</td><td class="num">%s</td></tr>
                    <tr><td><span class="bold">Estado:</span> %s</td><td class="num">Despacho: %s</td></tr>
                    <tr><td colspan="2"><span class="bold">NCF:</span> %s</td></tr>
                  </table>

                  <table class="items">
                    <thead>
                      <tr>
                        <th>Producto / Detalles</th>
                        <th class="num">Total</th>
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
                      <td>Costo Envío</td>
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
                      <td>TOTAL</td>
                      <td class="num">%s</td>
                    </tr>
                  </table>

                  <div class="divider"></div>
                  <p class="footer">¡Gracias por su compra!<br />Documento generado automaticamente.</p>
                </body>
                </html>
                """.formatted(
                factura.idVenta(),
                formatDate(factura.fechaHoraVenta()),
                escape(factura.clienteNombre()),
                escape(factura.clienteTelefono()),
                escape(factura.clienteCedula()),
                "",
                escape(factura.vendedorNombre()),
                "",
                escape(factura.estado()),
                factura.llevaDespacho() ? "Si" : "No",
                escape(factura.comprobanteFiscal()),
                lineas,
                formatMoney(factura.subtotal()),
                formatMoney(factura.impuestos()),
                formatMoney(factura.descuento()),
                formatMoney(factura.costoEnvio()),
                formatMoney(factura.montoCobrado()),
                formatMoney(factura.balancePendiente()),
                formatMoney(factura.montoTotal())
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