package com.agroveterinaria.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class CrudGridPaginator<T> extends HorizontalLayout {

    private final int pageSize;
    private final String itemLabel;
    private final Span summary = new Span();
    private final Button previous = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button next = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private Supplier<Collection<T>> source = List::of;
    private Runnable refreshOperation = () -> {};
    private int currentPage = 0;
    private int totalItems = 0;

    public CrudGridPaginator(int pageSize, String itemLabel) {
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
                refreshOperation.run();
            }
        });

        next.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        next.setAriaLabel("Pagina siguiente");
        next.addClickListener(event -> {
            if (currentPage < totalPages() - 1) {
                currentPage++;
                refreshOperation.run();
            }
        });

        add(summary, previous, next);
    }

    public void setSource(Supplier<Collection<T>> source) {
        this.source = source;
    }

    public void setRefreshOperation(Runnable refreshOperation) {
        this.refreshOperation = refreshOperation;
    }

    public void reset() {
        currentPage = 0;
        refreshOperation.run();
    }

    public Collection<T> pageItems() {
        List<T> items = new ArrayList<>(source.get());
        totalItems = items.size();

        if (currentPage >= totalPages()) {
            currentPage = Math.max(totalPages() - 1, 0);
        }

        int from = Math.min(currentPage * pageSize, totalItems);
        int to = Math.min(from + pageSize, totalItems);
        updateSummary(from, to);
        return items.subList(from, to);
    }

    private void updateSummary(int from, int to) {
        if (totalItems == 0) {
            summary.setText("Sin " + itemLabel + " para mostrar");
        } else {
            summary.setText(String.format("Mostrando %d-%d de %d %s", from + 1, to, totalItems, itemLabel));
        }

        previous.setEnabled(currentPage > 0);
        next.setEnabled(currentPage < totalPages() - 1);
    }

    private int totalPages() {
        return (int) Math.ceil(totalItems / (double) pageSize);
    }
}
