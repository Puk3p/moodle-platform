/** Teacher-facing proctoring summary for one attempt (GET /api/teacher/quizzes/attempts/:id/proctor). */
export interface ProctorReport {
  attemptId: number;
  studentName: string;
  studentEmail: string;
  leaveCount: number;
  totalAwaySeconds: number;
  pasteCount: number;
  timeline: ProctorTimelineEntry[];
}

export interface ProctorTimelineEntry {
  type: string;
  detail: string | null;
  occurredAt: string;
}
