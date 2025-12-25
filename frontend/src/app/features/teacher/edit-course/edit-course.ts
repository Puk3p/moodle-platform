import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-edit-course',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './edit-course.html',
  styleUrls: ['./edit-course.scss']
})
export class EditCourseComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  courseCodeParam = '';
  
  course = {
    title: '',
    code: '',
    term: 'Fall 2024',
    status: 'Published',
    description: ''
  };

  ngOnInit() {
    this.courseCodeParam = this.route.snapshot.paramMap.get('code') || '';
    
    if (this.courseCodeParam === 'CS201') {
      this.course = {
        title: 'Data Structures',
        code: 'CS201',
        term: 'Fall 2024',
        status: 'Published',
        description: 'Introduction to fundamental data structures and algorithms, including lists, stacks, queues, trees, and graphs.'
      };
    } else {
    }
  }

  setStatus(status: string) {
    this.course.status = status;
  }

  onSave() {
    console.log('Saving course...', this.course);
    //todo! sa facem save in db

    this.router.navigate(['/manage-courses']);
  }

  onCancel() {
    this.router.navigate(['/manage-courses']);
  }
}