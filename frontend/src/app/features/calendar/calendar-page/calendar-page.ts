import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common'; 
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faChevronLeft,
  faChevronRight,
  faFlask,
  faClipboardList,
  faCode,
  faPenToSquare,
  faCalendarCheck,
  IconDefinition
} from '@fortawesome/free-solid-svg-icons';
import { CalendarService } from '../../../core/services/calendar.service';
import { CalendarEvent } from '../../../core/models/calendar.model';

interface DayCell {
  dateObj: Date;       
  dayNumber: number;
  inCurrentRange: boolean;
  isoDate: string; 
  isToday: boolean;
}

@Component({
  selector: 'app-calendar-page',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  providers: [DatePipe],
  templateUrl: './calendar-page.html',
  styleUrl: './calendar-page.scss',
})
export class CalendarPageComponent implements OnInit {
  private calendarService = inject(CalendarService);
  private datePipe = inject(DatePipe);

  faChevronLeft = faChevronLeft;
  faChevronRight = faChevronRight;

  viewMode: 'month' | 'week' = 'month';
  currentDate: Date = new Date();
  
  weekdays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  
  events: CalendarEvent[] = [];
  days: DayCell[] = [];

  constructor() {
    this.generateGrid();
  }

  ngOnInit() {
    this.calendarService.getCalendarEvents().subscribe({
      next: (res) => {
        this.events = res.events;
      },
      error: (err) => console.error('Error loading calendar', err)
    });
  }


  get monthLabel(): string {
    if (this.viewMode === 'month') {
      return this.datePipe.transform(this.currentDate, 'MMMM yyyy') || '';
    } else {
      const start = this.getStartOfWeek(this.currentDate);
      const end = new Date(start);
      end.setDate(end.getDate() + 6);
      
      const startFormat = this.datePipe.transform(start, 'MMM d');
      const endFormat = this.datePipe.transform(end, 'MMM d, yyyy');
      return `${startFormat} - ${endFormat}`;
    }
  }

  get deadlines(): CalendarEvent[] {
    return this.events.sort((a, b) => a.date.localeCompare(b.date));
  }


  setView(mode: 'month' | 'week') {
    this.viewMode = mode;
    this.generateGrid();
  }

  goToday() {
    this.currentDate = new Date();
    this.generateGrid();
  }

  prev() {
    if (this.viewMode === 'month') {
      this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() - 1, 1);
    } else {
      this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth(), this.currentDate.getDate() - 7);
    }
    this.generateGrid();
  }

  next() {
    if (this.viewMode === 'month') {
      this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + 1, 1);
    } else {
      this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth(), this.currentDate.getDate() + 7);
    }
    this.generateGrid();
  }


  private generateGrid() {
    if (this.viewMode === 'month') {
      this.generateMonthGrid();
    } else {
      this.generateWeekGrid();
    }
  }

  private generateMonthGrid() {
    const year = this.currentDate.getFullYear();
    const month = this.currentDate.getMonth();
    
    const firstDayOfMonth = new Date(year, month, 1);
    const lastDayOfMonth = new Date(year, month + 1, 0);
    
    const startDayIndex = firstDayOfMonth.getDay();
    const totalDays = lastDayOfMonth.getDate();

    const cells: DayCell[] = [];

    const prevMonthLastDay = new Date(year, month, 0).getDate();
    for (let i = startDayIndex - 1; i >= 0; i--) {
      const d = prevMonthLastDay - i;
      const dateObj = new Date(year, month - 1, d);
      cells.push(this.createDayCell(dateObj, false));
    }

    for (let d = 1; d <= totalDays; d++) {
      const dateObj = new Date(year, month, d);
      cells.push(this.createDayCell(dateObj, true));
    }

    let nextD = 1;
    while (cells.length % 7 !== 0 || cells.length < 35) {
      const dateObj = new Date(year, month + 1, nextD++);
      cells.push(this.createDayCell(dateObj, false));
    }

    this.days = cells;
  }

  private generateWeekGrid() {
    const startOfWeek = this.getStartOfWeek(this.currentDate);
    const cells: DayCell[] = [];

    for (let i = 0; i < 7; i++) {
      const dateObj = new Date(startOfWeek);
      dateObj.setDate(startOfWeek.getDate() + i);
      cells.push(this.createDayCell(dateObj, true));
    }
    
    this.days = cells;
  }

  private createDayCell(date: Date, inRange: boolean): DayCell {
    const today = new Date();
    const isToday = date.getDate() === today.getDate() &&
                    date.getMonth() === today.getMonth() &&
                    date.getFullYear() === today.getFullYear();
    
    return {
      dateObj: date,
      dayNumber: date.getDate(),
      inCurrentRange: inRange,
      isoDate: this.toIsoDate(date),
      isToday: isToday
    };
  }

  private getStartOfWeek(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day;
    return new Date(d.setDate(diff));
  }

  private toIsoDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }


  getEventsForDay(day: DayCell): CalendarEvent[] {
    return this.events.filter(e => e.date === day.isoDate);
  }

  getIconForType(type: string): IconDefinition {
    if (!type) return faCalendarCheck;
    switch (type.toLowerCase()) {
      case 'lab': return faFlask;
      case 'assignment': return faClipboardList;
      case 'project': return faPenToSquare;
      case 'quiz': return faCode; 
      default: return faCalendarCheck;
    }
  }
}