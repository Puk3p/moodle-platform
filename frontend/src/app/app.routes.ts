import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { PublicHomeComponent } from './features/home/public-home/public-home';
import { DashboardHomeComponent } from './features/home/dashboard-home/dashboard-home';
import { ManageCoursesComponent } from './features/teacher/manage-courses/manage-courses';
import { EditCourseComponent } from './features/teacher/edit-course/edit-course';
import { EnrolledStudentsComponent } from './features/teacher/enrolled-students/enrolled-students';
import { CourseResourcesComponent } from './features/teacher/course-resources/course-resources';
import { CoursePreviewComponent } from './features/teacher/course-preview/course-preview';
import { UploadResourceComponent } from './features/teacher/upload-resource/upload-resource'; 
import { QuizResultsComponent } from './features/teacher/quiz-results/quiz-results';
import { QuizAttemptReviewComponent } from './features/teacher/quiz-attempt-review/quiz-attempt-review';
import { TakeQuizComponent } from './features/quiz/take-quiz/take-quiz';

export const routes: Routes = [
  {
    path: '',
    component: PublicHomeComponent
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then(m => m.Login),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register').then(m => m.Register),
  },
  {
    path: 'dashboard',
    component: DashboardHomeComponent,
    canActivate: [authGuard]
  },
  
  {
    path: 'courses',
    loadComponent: () => import('./features/courses/courses').then(m => m.CoursesComponent),
    canActivate: [authGuard]
  },
  {
    
    path: 'courses/:id',
    loadComponent: () => import('./features/courses/course-page/course-page').then(m => m.CoursePageComponent),
    canActivate: [authGuard]
  },
  {
    
    path: 'courses/:code/assignment/:id',
    loadComponent: () => import('./features/student/assignment-submit/assignment-submit').then(m => m.AssignmentSubmitComponent),
    canActivate: [authGuard]
  },
  
  {
    path: 'calendar',
    loadComponent: () => import('./features/calendar/calendar-page/calendar-page').then(m => m.CalendarPageComponent),
    canActivate: [authGuard]
  },
  {
    path: 'grades',
    loadComponent: () => import('./features/grades/grades-page/grades-page').then(m => m.GradesPageComponent),
    canActivate: [authGuard]
  },
  {
    path: 'resources',
    canActivate: [authGuard],
    loadComponent: () => import('./features/resources/resources-page/resources-page').then(m => m.ResourcesPageComponent)
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () => import('./features/settings/settings-page/settings-page').then(m => m.SettingsPageComponent)
  },

  
  {
    path: 'manage-courses',
    component: ManageCoursesComponent,
    canActivate: [authGuard]
  },
  {
    path: 'manage-quizzes',
    canActivate: [authGuard],
    loadComponent: () => import('./features/teacher/manage-quizzes/manage-quizzes').then(m => m.ManageQuizzesComponent)
  },
  {
    path: 'question-bank',
    loadComponent: () => import('./features/teacher/question-bank/question-bank').then(m => m.QuestionBankComponent),
    canActivate: [authGuard]
  },
  {
    path: 'upload-resource',
    component: UploadResourceComponent,
    canActivate: [authGuard]
  },
  {
    path: 'create-announcement',
    loadComponent: () => import('./features/teacher/create-announcement/create-announcement').then(m => m.CreateAnnouncementComponent),
    canActivate: [authGuard]
  },
  {
    path: 'gradebook',
    loadComponent: () => import('./features/admin/admin-gradebook/admin-gradebook').then(m => m.AdminGradebookComponent),
    canActivate: [authGuard]
  },
  {
    path: 'students',
    loadComponent: () => import('./features/admin/admin-students/admin-students').then(m => m.AdminStudentsComponent),
    canActivate: [authGuard] 
  },
  {
    path: 'manage-courses/:code/edit',
    component: EditCourseComponent,
    canActivate: [authGuard]
  },
  { 
    path: 'manage-courses/:code/students', 
    component: EnrolledStudentsComponent, 
    canActivate: [authGuard] 
  },
  {
    path: 'manage-courses/:code/resources',
    component: CourseResourcesComponent,
    canActivate: [authGuard]
  },
  { 
    path: 'manage-courses/:code/preview', 
    component: CoursePreviewComponent, 
    canActivate: [authGuard] 
  },
  {
    path: 'teacher/quizzes/:id/results',
    component: QuizResultsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'teacher/quizzes/attempts/:attemptId/review', 
    component: QuizAttemptReviewComponent,
    canActivate: [authGuard]
  },
  {
    path: 'assignments/submissions/:id/grade',
    loadComponent: () => import('./features/teacher/grade-assignment/grade-assignment').then(m => m.GradeAssignmentComponent),
    canActivate: [authGuard]
  },
  {
    
    path: 'manage-courses/:code/assignments/:id',
    loadComponent: () => import('./features/teacher/assignment-dashboard/assignment-dashboard').then(m => m.AssignmentDashboardComponent),
    canActivate: [authGuard]
},
  
  {
    path: 'take-quiz/:quizId',
    component: TakeQuizComponent,
    canActivate: [authGuard]
  },

  
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];