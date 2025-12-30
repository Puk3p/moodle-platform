export interface StudentOption {
  id: number;
  text: string;
}

export interface StudentQuestion {
  id: number;
  text: string;
  points: number;
  type: 'SINGLE_CHOICE' | 'MCQ_SINGLE' | 'TRUE_FALSE' | 'CODE' | 'FREE_TEXT' | 'DRAG_DROP';
  options: StudentOption[];
  imageUrl?: string;

  selectedOptionId?: number;
  textAnswer?: string;
  isFlagged?: boolean;
}

export interface StudentQuizView {
  attemptId: number;
  quizId: number;
  title: string;
  timeLimitMinutes: number;
  questions: StudentQuestion[];
}

export interface QuizSubmission {
  attemptId: number;
  answers: { 
    questionId: number; 
    selectedOptionId?: number; 
    textAnswer?: string;
    orderedOptionIds?: number[];
  }[];
}

export interface QuizResult {
  attemptId: number;
  quizTitle: string;
  score: number;
  maxScore: number;
  passed: boolean;
  completedAt: string;
}