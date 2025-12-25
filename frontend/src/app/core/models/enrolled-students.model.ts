export interface EnrolledStudentsResponse {
  courseCode: string;
  stats: StudentStats;
  students: Student[];
}

export interface StudentStats {
  total: number;
  activeRate: number;
  pending: number;
}

export interface Student {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  group: string;
  progress: number;
  lastActivity: string;
  avatarColor: string;
}