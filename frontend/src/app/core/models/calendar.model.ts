export interface CalendarEvent {
  id: number;
  date: string;
  title: string;
  courseCode: string;
  type: string;
}

export interface CalendarResponse {
  events: CalendarEvent[];
}