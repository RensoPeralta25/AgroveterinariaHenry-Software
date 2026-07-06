import { Calendar } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import esLocale from '@fullcalendar/core/locales/es';

class CitaCalendar extends HTMLElement {
  constructor() {
    super();
    this.calendar = null;
    this.calendarHost = null;
    this.pendingAppointments = [];
  }

  connectedCallback() {
    this.ensureDom();
    this.upgradeProperty('appointments');
    if (!this.calendar) {
      this.renderCalendar();
    }
    this.loadAppointmentsFromDom();
    this.scheduleSizeUpdate();
  }

  disconnectedCallback() {
    if (this.calendar) {
      this.calendar.destroy();
      this.calendar = null;
    }
  }

  set appointments(value) {
    this.setAppointments(value);
  }

  setAppointments(value) {
    const appointments = this.parseAppointments(value);
    this.pendingAppointments = appointments;

    if (this.calendar) {
      this.applyAppointments(appointments);
    }
  }

  loadAppointmentsFromDom() {
    const dataElement = this.querySelector('script[data-cita-appointments]');
    if (dataElement) {
      this.setAppointments(dataElement.textContent || '[]');
    }
  }

  parseAppointments(value) {
    if (typeof value === 'string') {
      return JSON.parse(value || '[]');
    }

    return value || [];
  }

  applyAppointments(appointments) {
    this.calendar.removeAllEvents();
    this.syncVisibleDate(appointments);
    this.calendar.addEventSource(appointments);
    this.scheduleSizeUpdate();
  }

  ensureDom() {
    if (!this.calendarHost) {
      this.calendarHost = document.createElement('div');
      this.calendarHost.className = 'cita-calendar-host';
      this.appendChild(this.calendarHost);
    }
  }

  upgradeProperty(prop) {
    if (Object.prototype.hasOwnProperty.call(this, prop)) {
      const value = this[prop];
      delete this[prop];
      this[prop] = value;
    }
  }

  scheduleSizeUpdate() {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        if (this.calendar) {
          this.calendar.updateSize();
        }
      });
    });
  }

  syncVisibleDate(appointments) {
    if (appointments.length === 0 || !this.calendar) {
      return;
    }

    const starts = appointments
      .map((appointment) => appointment.start)
      .filter(Boolean)
      .sort();
    const firstAppointment = starts[0];
    const activeStart = this.calendar.view.activeStart;
    const activeEnd = this.calendar.view.activeEnd;
    const hasVisibleAppointment = starts.some((start) => {
      const date = new Date(start);
      return date >= activeStart && date < activeEnd;
    });

    if (firstAppointment && !hasVisibleAppointment) {
      this.calendar.gotoDate(firstAppointment);
    }
  }

  renderCalendar() {
    this.ensureDom();
    this.calendar = new Calendar(this.calendarHost, {
      plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
      locale: esLocale,
      initialView: 'timeGridWeek',
      firstDay: 1,
      height: '100%',
      expandRows: true,
      nowIndicator: true,
      editable: true,
      selectable: true,
      selectMirror: true,
      eventStartEditable: true,
      eventDurationEditable: false,
      slotMinTime: '07:00:00',
      slotMaxTime: '20:00:00',
      slotDuration: '00:30:00',
      eventMaxStack: 3,
      allDaySlot: false,
      headerToolbar: {
        left: 'prev,next today',
        center: 'title',
        right: 'dayGridMonth,timeGridWeek,timeGridDay',
      },
      buttonText: {
        today: 'Hoy',
        month: 'Mes',
        week: 'Semana',
        day: 'Dia',
      },
      select: (info) => {
        this.$server.createAppointment(info.startStr);
        this.calendar.unselect();
      },
      dateClick: (info) => {
        this.$server.createAppointment(info.dateStr);
      },
      eventClick: (info) => {
        this.$server.editAppointment(Number(info.event.id));
      },
      eventDrop: (info) => {
        this.$server.moveAppointment(Number(info.event.id), info.event.startStr)
          .then((saved) => {
            if (!saved) {
              info.revert();
            }
          })
          .catch(() => info.revert());
      },
      eventClassNames: (info) => info.event.extendedProps.realizado ? ['cita-realizada'] : ['cita-pendiente'],
      eventDidMount: (info) => {
        info.el.title = info.event.extendedProps.tooltip || info.event.title;
      },
    });

    this.calendar.render();
    if (this.pendingAppointments.length > 0) {
      this.applyAppointments(this.pendingAppointments);
    }
    this.scheduleSizeUpdate();
  }
}

customElements.define('cita-calendar', CitaCalendar);
