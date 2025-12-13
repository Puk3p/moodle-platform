export type ResourceFileType =
  | 'pdf'
  | 'doc'
  | 'slides'
  | 'zip'
  | 'link'
  | 'video';

export interface ResourceFile {
  id: string;
  title: string;
  sizeLabel: string;
  type: ResourceFileType;
  url?: string;
}

export interface CourseResources {
  courseCode: string;
  courseName: string;
  files: ResourceFile[];
}
