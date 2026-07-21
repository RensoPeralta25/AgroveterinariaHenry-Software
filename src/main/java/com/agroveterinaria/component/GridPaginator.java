package com.agroveterinaria.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GridPaginator<T> extends HorizontalLayout {

    private final Grid<T> grid;
    private final int pageSize;
    private final String itemLabel;
    private final Span summary = new Span();
    private final Button previous = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button next = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private List<T> items = List.of();
    private int currentPage = 0;

    public GridPaginator(Grid<T> grid, int pageSize, String itemLabel) {
        this.grid = grid;
        this.pageSize = pageSize;
        this.itemLabel = itemLabel;

        addClassName("grid-pagination");
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        summary.addClassName("grid-pagination-summary");

        previous.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        previous.setAriaLabel("Pagina anterior");
        previous.addClickListener(event -> {
            if (currentPage > 0) {
                currentPage--;
                updateGrid();
            }
        });

        next.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        next.setAriaLabel("Pagina siguiente");
        next.addClickListener(event -> {
            if (currentPage < totalPages() - 1) {
                currentPage++;
                updateGrid();
            }
        });

        add(summary, previous, next);
    }

    public void setItems(Collection<T> newItems) {
        items = new ArrayList<>(newItems);
        currentPage = 0;
        updateGrid();
    }

    public void refresh() {
        if (currentPage >= totalPages()) {
            currentPage = Math.max(totalPages() - 1, 0);
        }
        updateGrid();
    }

    private void updateGrid() {
        int total = items.size();
        int from = Math.min(currentPage * pageSize, total);
        int to = Math.min(from + pageSize, total);

        grid.setItems(items.subList(from, to));

        if (total == 0) {
            summary.setText("Sin " + itemLabel + " para mostrar");
        } else {
            summary.setText(String.format("Mostrando %d-%d de %d %s", from + 1, to, total, itemLabel));
        }

        previous.setEnabled(currentPage > 0);
        next.setEnabled(currentPage < totalPages() - 1);
    }

    private int totalPages() {
        return (int) Math.ceil(items.size() / (double) pageSize);
    }
}
