import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faClipboardList,
  faClipboardQuestion,
  faCode,
  faBullhorn,
  faCheckCircle,
  faComments,
  faCertificate,
  faBoxArchive,
  faTrophy,
} from '@fortawesome/free-solid-svg-icons';
import { DashboardService } from '../../../core/services/dashboard.service';
import {
  ActiveCourse,
  CompletedCourse,
  Deadline as DeadlineDto,
  Activity as ActivityDto,
  DashboardHomeResponse,
} from '../../../core/models/dashboard.model';

type Deadline = DeadlineDto & { icon: any };
type Activity = ActivityDto & { icon: any };

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
})
export class DashboardHomeComponent implements OnInit {
  userName = 'Student';

  faCheckCircle = faCheckCircle;
  faCertificate = faCertificate;
  faBoxArchive = faBoxArchive;
  faTrophy = faTrophy;

  activeCourses: ActiveCourse[] = [];
  completedCourses: CompletedCourse[] = [];
  deadlines: Deadline[] = [];
  activities: Activity[] = [];

  loading = true;
  error?: string;

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  private loadDashboard(): void {
    this.dashboardService.getMyDashboard().subscribe({
      next: (res: DashboardHomeResponse) => {
        this.loading = false;

        this.userName = res.userName || 'Student';
        this.activeCourses = res.activeCourses;
        this.completedCourses = res.completedCourses;

        const deadlineIcons = [faClipboardList, faClipboardQuestion, faCode];
        this.deadlines = res.deadlines.map((d, index) => ({
          ...d,
          icon: deadlineIcons[index] ?? faClipboardList,
        }));

        const activityIcons = [faBullhorn, faCheckCircle, faComments];
        this.activities = res.activities.map((a, index) => ({
          ...a,
          icon: activityIcons[index] ?? faBullhorn,
        }));
      },
      error: (err) => {
        this.loading = false;
        console.error('Failed to load dashboard', err);
        this.error = 'Could not load dashboard data.';
      },
    });
  }
}
