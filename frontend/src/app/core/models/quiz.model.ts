export interface Quiz {
  id: string;
  title: string;
  courseName: string;
  context: string;
  status: 'Published' | 'Draft';
  questionsCount: number;
  durationMinutes: number;
  attemptsLabel: string;
  lastUpdated: string;
}