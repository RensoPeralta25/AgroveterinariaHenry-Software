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

    this.chart = new Chart(this.canvas, this.chartConfig);
  }
}

customElements.define('dashboard-chart', DashboardChart);
