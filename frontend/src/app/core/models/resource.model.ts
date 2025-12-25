export interface ResourceFile {
  id: string;
  title: string;
  sizeLabel: string;
  type: 'pdf' | 'doc' | 'zip' | 'slides' | 'link' | 'video';
}

export interface CourseResources {
  courseCode: string;
  courseName: string;
  files: ResourceFile[];
}

export interface ResourcesPageResponse {
  courses: CourseResources[];
}

export interface Resource {
  id: number;
  name: string;
  category: string;
  type: string;
  size: string;
  date: string;
}