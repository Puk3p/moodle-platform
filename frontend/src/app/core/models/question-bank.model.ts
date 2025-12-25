export type QuestionType = 'Code' | 'Multi Choice' | 'True / False' | 'Drag & Drop';
export type Difficulty = 'Easy' | 'Medium' | 'Hard';

export interface Question {
  id: string;
  text: string;
  tags: string[];
  type: QuestionType;
  difficulty: Difficulty;
  usageCount: number;
}

export interface Category {
  id: string;
  name: string;
  count?: number;
  level: number;
  isOpen?: boolean;
}