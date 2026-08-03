import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

class DashboardChart extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({ mode: 'open' });
    this.shadowRoot.innerHTML = `
      <style>
        :host {
          display: block;
          width: 100%;
          height: 100%;
          min-height: 260px;
        }

        canvas {
          display: block;
          width: 100% !important;
          height: 100% !important;
        }
      </style>
      <canvas></canvas>
    `;
    this.canvas = this.shadowRoot.querySelector('canvas');
    this.chart = null;
    this.chartConfig = null;
  }

  set config(value) {
    this.chartConfig = typeof value === 'string' ? JSON.parse(value) : value;
    this.renderChart();
  }

  get config() {
    return this.chartConfig;
  }

  connectedCallback() {
    this.renderChart();
  }

  disconnectedCallback() {
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }
  }

  renderChart() {
    if (!this.isConnected || !this.chartConfig || !this.canvas) {
      return;
    }

    if (this.chart) {
      this.chart.destroy();
    }

    const config = this.prepareConfig(this.chartConfig);
    this.chart = new Chart(this.canvas, config);
  }

  prepareConfig(sourceConfig) {
    const config = JSON.parse(JSON.stringify(sourceConfig));
    const valueFormat = config.valueFormat;
    delete config.valueFormat;

    const numberFormatter = new Intl.NumberFormat('es-DO', {
      notation: 'compact',
      maximumFractionDigits: 1,
    });
    const formatValue = (value) => {
      const compactValue = numberFormatter.format(Number(value));
      return valueFormat === 'currency' ? `RD$ ${compactValue}` : compactValue;
    };

    config.options ??= {};
    config.options.plugins ??= {};
    config.options.plugins.tooltip ??= {};
    config.options.plugins.tooltip.callbacks = {
      label: (context) => {
        const label = context.label || context.dataset.label || '';
        const suffix = valueFormat === 'count' ? ' productos' : '';
        return `${label}: ${formatValue(context.raw)}${suffix}`;
      },
    };

    if (config.options.scales?.y) {
      config.options.scales.y.ticks = {
        callback: (value) => formatValue(value),
      };
    }

    return config;
  }
}

customElements.define('dashboard-chart', DashboardChart);
