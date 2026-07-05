package com.agroveterinaria.component;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CantidadFraccionadaField extends CustomField<BigDecimal> {

    private final IntegerField txtCajas = new IntegerField("Cajas / Paquetes");
    private final BigDecimalField txtUnidades = new BigDecimalField("Unidades / Fracciones");

    private BigDecimal factorConversion = BigDecimal.ONE;
    private boolean permiteFraccionamiento = true;
    private boolean esGranel = false;

    public CantidadFraccionadaField() {
        txtCajas.setPlaceholder("0");
        txtCajas.setMin(0);
        txtCajas.setWidth("130px");

        txtUnidades.setPlaceholder("0");
        txtUnidades.setWidth("150px");

        HorizontalLayout layout = new HorizontalLayout(txtCajas, txtUnidades);
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.setMargin(false);
        layout.setAlignItems(FlexComponent.Alignment.BASELINE);
        layout.getStyle().set("margin-top", "8px");
        layout.getStyle().set("margin-bottom", "8px");
        this.getStyle().set("padding-top", "0");
        this.getStyle().set("margin-top", "0");
        add(layout);

        txtCajas.addValueChangeListener(e -> updateValue());
        txtUnidades.addValueChangeListener(e -> {
            normalizarUnidades();
            updateValue();
        });
    }


    public void configurarProducto(BigDecimal factor, boolean permiteFraccionamiento, boolean esGranel) {
        this.factorConversion = (factor != null && factor.compareTo(BigDecimal.ZERO) > 0) ? factor : BigDecimal.ONE;
        this.permiteFraccionamiento = permiteFraccionamiento;
        this.esGranel = esGranel;

        if (this.esGranel) {
            txtCajas.setVisible(false);
            txtUnidades.setVisible(true);
            txtUnidades.setLabel("Cantidad Decimal");
            txtUnidades.setWidthFull();
        } else if (!this.permiteFraccionamiento) {
            txtCajas.setVisible(true);
            txtCajas.setLabel("Cantidad (Unidades)");
            txtCajas.setWidthFull();
            txtUnidades.setVisible(false);
        } else {
            txtCajas.setVisible(true);
            txtCajas.setLabel("Empaques");
            txtCajas.setWidth("100px");
            txtCajas.getStyle().set("margin-right", "15px");
            txtUnidades.setVisible(true);
            txtUnidades.setLabel("Unidades");
            txtUnidades.setWidth("100px");
        }
        clear();
    }

    private void normalizarUnidades() {
        if (esGranel || !permiteFraccionamiento || factorConversion.compareTo(BigDecimal.ONE) <= 0) {
            return;
        }

        BigDecimal unidadesIngresadas = txtUnidades.getValue();
        if (unidadesIngresadas != null && unidadesIngresadas.compareTo(factorConversion) >= 0) {
            BigDecimal cajasExtras = unidadesIngresadas.divide(factorConversion, 0, RoundingMode.DOWN);
            BigDecimal residuoUnidades = unidadesIngresadas.remainder(factorConversion);

            int cajasActuales = txtCajas.getValue() != null ? txtCajas.getValue() : 0;

            txtCajas.setValue(cajasActuales + cajasExtras.intValue());
            txtUnidades.setValue(residuoUnidades);
        }
    }

    @Override
    protected BigDecimal generateModelValue() {
        Integer cajas = txtCajas.getValue();
        BigDecimal unidades = txtUnidades.getValue();

        if (cajas == null && unidades == null) {
            return null;
        }

        if (esGranel) {
            return unidades != null ? unidades : BigDecimal.ZERO;
        }

        BigDecimal totalBaseDesdeCajas = cajas != null ? BigDecimal.valueOf(cajas).multiply(factorConversion) : BigDecimal.ZERO;
        BigDecimal totalBaseDesdeUnidades = unidades != null ? unidades : BigDecimal.ZERO;

        return totalBaseDesdeCajas.add(totalBaseDesdeUnidades);
    }

    @Override
    protected void setPresentationValue(BigDecimal newModelValue) {
        if (newModelValue == null || newModelValue.compareTo(BigDecimal.ZERO) <= 0) {
            txtCajas.clear();
            txtUnidades.clear();
            return;
        }

        if (esGranel) {
            txtUnidades.setValue(newModelValue);
            txtCajas.clear();
        } else if (!permiteFraccionamiento || factorConversion.compareTo(BigDecimal.ONE) <= 0) {
            txtCajas.setValue(newModelValue.intValue());
            txtUnidades.clear();
        } else {
            BigDecimal[] divisionYResiduo = newModelValue.divideAndRemainder(factorConversion);
            txtCajas.setValue(divisionYResiduo[0].intValue());
            txtUnidades.setValue(divisionYResiduo[1]);
        }
    }
}