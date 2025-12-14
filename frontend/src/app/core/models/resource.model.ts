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