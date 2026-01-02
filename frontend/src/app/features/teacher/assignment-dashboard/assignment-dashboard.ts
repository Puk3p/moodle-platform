import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ResourcesService } from '../../../core/services/resources.service';

@Component({
  selector: 'app-assignment-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './assignment-dashboard.html',
  styleUrls: ['./assignment-dashboard.scss']
})
export class AssignmentDashboardComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(ResourcesService);

  assignmentId!: number;
  data: any = null;
  isLoading = true;

  ngOnInit() {
    this.assignmentId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadData();
  }

  loadData() {
    this.service.getAssignmentOverview(this.assignmentId).subscribe({
      next: (res) => {
        this.data = res;
        this.isLoading = false;
      },
      error: () => this.isLoading = false
    });
  }

  gradeStudent(submissionId: number | null) {
    if (submissionId) {
      
      this.router.navigate(['/assignments/submissions', submissionId, 'grade']);
    } else {
      alert('Student has not submitted yet.');
    }
  }
}