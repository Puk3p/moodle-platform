export interface CourseModuleItem {
  type: string;
  label: string;
}

export interface CourseModule {
  title: string;
  description: string;
  items: CourseModuleItem[];
}

export interface CourseStats {
  overallProgress: number;
  completedLabs: number;
  totalLabs: number;
  averageGrade: string;
}

export interface CourseDeadline {
  title: string;
  context: string;
  due: string;
  type: string;
}

export interface CourseAnnouncement {
  title: string;
  body: string;
  meta: string;
  last: boolean;
}

export interface CourseDetailsResponse {
  courseCode: string;
  fullTitle: string;
  termLabel: string;
  instructor: string;
  stats: CourseStats;
  modules: CourseModule[];
  deadlines: CourseDeadline[];
  announcements: CourseAnnouncement[];
}