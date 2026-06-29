package com.agroveterinaria.view.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.agroveterinaria.dto.dashboard.DashboardSeriesPointDTO;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag("dashboard-chart")
@JsModule("./dashboard-chart.js")
public class DashboardChart extends Component {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> DOUGHNUT_COLORS = List.of(
            "#1676f3",
            "#0f766e",
            "#f97316",
            "#dc2626",
            "#7c3aed",
            "#64748b"
    );

    public DashboardChart(
            String tipo,
            String etiquetaDataset,
            List<DashboardSeriesPointDTO> puntos,
            String colorPrincipal,
            String colorSecundario
    ) {
        addClassName("dashboard-chart");
        setChartConfig(tipo, etiquetaDataset, puntos, colorPrincipal, colorSecundario);
    }

    public void setChartConfig(
            String tipo,
            String etiquetaDataset,
            List<DashboardSeriesPointDTO> puntos,
            String colorPrincipal,
            String colorSecundario
    ) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("type", tipo);

        Map<String, Object> dataset = new LinkedHashMap<>();
        dataset.put("label", etiquetaDataset);
        dataset.put("data", crearValores(puntos));
        dataset.put("borderColor", colorPrincipal);
        dataset.put("backgroundColor", "doughnut".equals(tipo) ? crearPaleta(puntos.size()) : colorSecundario);
        dataset.put("borderWidth", 2);
        dataset.put("tension", 0.35);
        dataset.put("fill", !"bar".equals(tipo));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("labels", crearLabels(puntos));
        data.put("datasets", List.of(dataset));
        config.put("data", data);

        Map<String, Object> legend = Map.of("display", false);
        Map<String, Object> plugins = Map.of("legend", legend);
        Map<String, Object> y = Map.of("beginAtZero", true);
        Map<String, Object> scales = Map.of("y", y);
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("responsive", true);
        options.put("maintainAspectRatio", false);
        options.put("plugins", plugins);
        if (!"doughnut".equals(tipo)) {
            options.put("scales", scales);
        }
        config.put("options", options);

        getElement().setProperty("config", toJson(config));
    }

    private List<String> crearLabels(List<DashboardSeriesPointDTO> puntos) {
        return puntos.stream()
                .map(DashboardSeriesPointDTO::etiqueta)
                .toList();
    }

    private List<Double> crearValores(List<DashboardSeriesPointDTO> puntos) {
        return puntos.stream()
                .map(punto -> punto.valor().doubleValue())
                .toList();
    }

    private List<String> crearPaleta(int total) {
        return java.util.stream.IntStream.range(0, total)
                .mapToObj(index -> DOUGHNUT_COLORS.get(index % DOUGHNUT_COLORS.size()))
                .toList();
    }

    private String toJson(Map<String, Object> config) {
        try {
            return OBJECT_MAPPER.writeValueAsString(config);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo preparar la configuracion del grafico", exception);
        }
    }
}
