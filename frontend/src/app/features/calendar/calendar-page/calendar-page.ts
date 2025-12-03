import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faChevronLeft,
  faChevronRight,
  faFlask,
  faClipboardList,
  faCode,
  faPenToSquare,
} from '@fortawesome/free-solid-svg-icons';

interface CalendarEvent {
  id: number;
  date: string;      // YYYY-MM-DD
  title: string;
  courseCode: string;
}

interface DayCell {
  dayNumber: number | null;
  inCurrentMonth: boolean;
  date?: string;
  isToday?: boolean;
}

@Component({
  selector: 'app-calendar-page',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  templateUrl: './calendar-page.html',
  styleUrl: './calendar-page.scss',
})
export class CalendarPageComponent {

  // icons
  faChevronLeft = faChevronLeft;
  faChevronRight = faChevronRight;
  faLab = faFlask;
  faAssignment = faClipboardList;
  faProject = faPenToSquare;
  faCode = faCode;

  readonly year = 2024;
  readonly month = 9;
  readonly monthLabel = 'October 2024';

  weekdays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  events: CalendarEvent[] = [
    { id: 1, date: '2024-10-09', title: 'Lab 4: Trees', courseCode: 'CS201' },
    { id: 2, date: '2024-10-09', title: 'Project Proposal', courseCode: 'CS350' },
    { id: 3, date: '2024-10-17', title: 'Assignment 3', courseCode: 'CS201' },
    { id: 4, date: '2024-10-23', title: 'Quiz 2', courseCode: 'CS201' },
    { id: 5, date: '2024-10-30', title: 'Midterm Exam', courseCode: 'CS201' },
  ];

  get deadlines(): CalendarEvent[] {
    return this.events;
  }

  days: DayCell[] = [];

  constructor() {
    this.days = this.generateDaysForMonth(this.year, this.month);
  }

  private generateDaysForMonth(year: number, month: number): DayCell[] {
    const cells: DayCell[] = [];

    const firstOfMonth = new Date(year, month, 1);
    const startWeekday = firstOfMonth.getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const daysInPrevMonth = new Date(year, month, 0).getDate();

    for (let i = startWeekday - 1; i >= 0; i--) {
      const dayNumber = daysInPrevMonth - i;
      cells.push({
        dayNumber,
        inCurrentMonth: false
      });
    }

    for (let d = 1; d <= daysInMonth; d++) {
      const isoDate = this.toIsoDate(year, month, d);
      cells.push({
        dayNumber: d,
        inCurrentMonth: true,
        date: isoDate,
        isToday: d === 20,
      });
    }

    let nextDay = 1;
    while (cells.length % 7 !== 0) {
      cells.push({
        dayNumber: nextDay++,
        inCurrentMonth: false
      });
    }

    return cells;
  }

  private toIsoDate(year: number, month: number, day: number): string {
    const m = String(month + 1).padStart(2, '0');
    const d = String(day).padStart(2, '0');
    return `${year}-${m}-${d}`;
  }

  getEventsForDay(day: DayCell): CalendarEvent[] {
    if (!day.date) {
      return [];
    }
    return this.events.filter(e => e.date === day.date);
  }
}
