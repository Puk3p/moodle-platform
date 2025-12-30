export interface CoursePreview {
  courseCode: string;
  title: string;
  term: string;
  instructorName: string;
  modules: PreviewModule[];
  announcements: PreviewAnnouncement[];
  deadlines: PreviewDeadline[];
  quizzes: PreviewItem[];
}

export interface PreviewModule {
  id: number;
  title: string;
  dateRange: string;
  status: 'completed' | 'current' | 'locked' | 'unlocked';
  unlockDate: string;
  items: PreviewItem[];
}

export interface PreviewItem {
  id: number;
  title: string;
  type: string;  
  meta: string; 
  isAssignment: boolean;
}

export interface PreviewAnnouncement {
  title: string;
  body: string;
  timeAgo: string;
}

export interface PreviewDeadline {
  title: string;
  due: string;
  urgent: boolean;
  icon: string;
}