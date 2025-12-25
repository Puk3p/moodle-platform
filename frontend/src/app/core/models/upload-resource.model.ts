export interface UploadOptions {
  courses: CourseOption[];
}

export interface CourseOption {
  code: string;
  name: string;
  modules: ModuleOption[];
}

export interface ModuleOption {
  id: number;
  title: string;
}