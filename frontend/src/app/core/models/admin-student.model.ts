export interface AdminStudent {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  className: string;
  classId: number | null;
  twoFaEnabled: boolean;
}

export interface UpdateStudentRequest {
  firstName: string;
  lastName: string;
  email: string;
  classId: number | null;
}