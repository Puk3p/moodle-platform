export interface ModuleEdit {
  id: number;
  title: string;
  description: string;
  sortOrder: number;
  
  startDate?: string; 
  endDate?: string;
  status?: 'Published' | 'Draft';

  stats?: {
    lectures: number;
    quizzes: number;
    labs: number;
  };
}

export interface CourseEdit {
  id: number;
  code: string;
  title: string;
  term: string;
  status: 'Published' | 'Draft';
  description: string;
  modules: ModuleEdit[];
  selectedGroupIds: number[];
}