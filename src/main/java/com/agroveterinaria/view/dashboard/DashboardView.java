package com.agroveterinaria.view.dashboard;

import com.agroveterinaria.dto.dashboard.DashboardAlertDTO;
import com.agroveterinaria.dto.dashboard.DashboardCategoryDTO;
import com.agroveterinaria.dto.dashboard.DashboardDataDTO;
import com.agroveterinaria.dto.dashboard.DashboardMetricDTO;
import com.agroveterinaria.dto.dashboard.DashboardSeriesPointDTO;
import com.agroveterinaria.service.DashboardService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

public class DashboardView extends Div {

    private final transient DashboardService dashboardService;

    public DashboardView(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
        addClassName("dashboard-view");
        render();
    }

    private void render() {
        removeAll();

        DashboardDataDTO data = dashboardService.obtenerResumenPrincipal();

        Button refreshButton = new Button(VaadinIcon.REFRESH.create());
        refreshButton.addClassName("dashboard-refresh-button");
        refreshButton.setAriaLabel("Actualizar dashboard");
        refreshButton.addClickListener(event -> render());

        HorizontalLayout header = new HorizontalLayout(createSectionTitle("Resumen operativo", "Indicadores clave del negocio"), refreshButton);
        header.addClassName("dashboard-toolbar");
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        add(header, createMetricsGrid(data.metricas()), createChartsGrid(data), createAlertsPanel(data.alertas()));
    }

    private Div createMetricsGrid(List<DashboardMetricDTO> metricas) {
        Div grid = new Div();
        grid.addClassName("dashboard-metrics-grid");
        metricas.forEach(metric -> grid.add(createMetric(metric)));
        return grid;
    }

    private Div createMetric(DashboardMetricDTO metric) {
        Div card = new Div();
        card.addClassNames("dashboard-metric", "dashboard-metric-" + metric.tono());

        Span value = new Span(metric.valor());
        value.addClassName("dashboard-metric-value");

        Span title = new Span(metric.titulo());
        title.addClassName("dashboard-metric-title");

        Span detail = new Span(metric.detalle());
        detail.addClassName("dashboard-metric-detail");

        card.add(value, title, detail);
        return card;
    }

    private Div createChartsGrid(DashboardDataDTO data) {
        Div grid = new Div();
        grid.addClassName("dashboard-charts-grid");
        grid.add(
                createChartPanel("Ventas netas recientes", "Últimos 14 días", data.ventasUltimosDias(), new DashboardChart(
                        "line",
                        "Ventas netas",
                        data.ventasUltimosDias(),
                        "#1676f3",
                        "rgba(22, 118, 243, 0.14)",
                        "currency"
                )),
                createChartPanel("Compras confirmadas", "Últimos 14 días", data.comprasUltimosDias(), new DashboardChart(
                        "bar",
                        "Compras",
                        data.comprasUltimosDias(),
                        "#0f766e",
                        "rgba(15, 118, 110, 0.28)",
                        "currency"
                )),
                createChartPanel("Gastos operativos", "Últimos 14 días", data.gastosOperativosUltimosDias(), new DashboardChart(
                        "bar",
                        "Gastos operativos",
                        data.gastosOperativosUltimosDias(),
                        "#dc2626",
                        "rgba(220, 38, 38, 0.24)",
                        "currency"
                )),
                createChartPanel("Inventario por categoría", "Productos distintos con existencias", toSeriesPoints(data.inventarioPorCategoria()), new DashboardChart(
                        "doughnut",
                        "Productos",
                        toSeriesPoints(data.inventarioPorCategoria()),
                        "#f97316",
                        "rgba(249, 115, 22, 0.32)",
                        "count"
                ))
        );
        return grid;
    }

    private Div createChartPanel(
            String title,
            String subtitle,
            List<DashboardSeriesPointDTO> puntos,
            DashboardChart chart
    ) {
        Div panel = new Div();
        panel.addClassName("dashboard-chart-panel");
        panel.add(createSectionTitle(title, subtitle));

        if (puntos.stream().noneMatch(punto -> punto.valor().compareTo(java.math.BigDecimal.ZERO) != 0)) {
            Div empty = new Div();
            empty.addClassName("dashboard-chart-empty");
            empty.setText("No hay datos registrados para este período.");
            panel.add(empty);
        } else {
            panel.add(chart);
        }
        return panel;
    }

    private Div createAlertsPanel(List<DashboardAlertDTO> alertas) {
        Div panel = new Div();
        panel.addClassName("dashboard-alerts-panel");
        panel.add(createSectionTitle("Alertas", "Seguimiento rapido de puntos sensibles"));

        Div list = new Div();
        list.addClassName("dashboard-alerts-list");

        if (alertas.isEmpty()) {
            Span empty = new Span("No hay alertas operativas por el momento.");
            empty.addClassName("dashboard-alert-empty");
            list.add(empty);
        } else {
            alertas.forEach(alerta -> list.add(createAlert(alerta)));
        }

        panel.add(list);
        return panel;
    }

    private Div createAlert(DashboardAlertDTO alerta) {
        Div item = new Div();
        item.addClassNames("dashboard-alert", "dashboard-alert-" + alerta.severidad());

        Span type = new Span(alerta.tipo());
        type.addClassName("dashboard-alert-type");

        Span title = new Span(alerta.titulo());
        title.addClassName("dashboard-alert-title");

        Span detail = new Span(alerta.detalle());
        detail.addClassName("dashboard-alert-detail");

        item.add(type, title, detail);
        return item;
    }

    private VerticalLayout createSectionTitle(String title, String subtitle) {
        H3 heading = new H3(title);
        heading.addClassName("dashboard-section-title");

        Span copy = new Span(subtitle);
        copy.addClassName("dashboard-section-subtitle");

        VerticalLayout text = new VerticalLayout(heading, copy);
        text.addClassName("dashboard-section-heading");
        text.setPadding(false);
        text.setSpacing(false);
        return text;
    }

    private List<DashboardSeriesPointDTO> toSeriesPoints(List<DashboardCategoryDTO> categorias) {
        return categorias.stream()
                .map(categoria -> new DashboardSeriesPointDTO(categoria.categoria(), categoria.valor()))
                .toList();
    }
}
