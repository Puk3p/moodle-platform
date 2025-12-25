import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

interface Student {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  group: string;
  progress: number;
  lastActivity: string;
  avatarColor: string;
}

@Component({
  selector: 'app-enrolled-students',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './enrolled-students.html',
  styleUrls: ['./enrolled-students.scss']
})
export class EnrolledStudentsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  
  courseCode = '';
  searchTerm = '';

  stats = {
    total: 124,
    activeRate: 98,
    pending: 14
  };

  students: Student[] = [
    { id: 1, firstName: 'Sarah', lastName: 'Jenkins', email: 'sarah.j@university.edu', group: 'Group A', progress: 92, lastActivity: 'Today, 10:42 AM', avatarColor: '#eff6ff' },
    { id: 2, firstName: 'Michael', lastName: 'Chen', email: 'm.chen22@university.edu', group: 'Group B', progress: 78, lastActivity: 'Yesterday', avatarColor: '#fdf2f8' }, 
    { id: 3, firstName: 'Emma', lastName: 'Larson', email: 'emma.l@university.edu', group: 'Group A', progress: 95, lastActivity: '2 hours ago', avatarColor: '#ecfdf5' },
    { id: 4, firstName: 'David', lastName: 'Kim', email: 'd.kim@university.edu', group: 'Group C', progress: 45, lastActivity: '3 days ago', avatarColor: '#fffbeb' },
    { id: 5, firstName: 'Jessica', lastName: 'Smith', email: 'j.smith@university.edu', group: 'Group B', progress: 88, lastActivity: 'Yesterday', avatarColor: '#f3e8ff' }, 
    { id: 6, firstName: 'Ryan', lastName: 'Brooks', email: 'ryan.b@university.edu', group: 'Group A', progress: 62, lastActivity: '5 days ago', avatarColor: '#ecfeff' }, 
  ];

  ngOnInit() {
    this.courseCode = this.route.snapshot.paramMap.get('code') || 'CS201';
  }

  getInitials(s: Student): string {
    return (s.firstName[0] + s.lastName[0]).toUpperCase();
  }

  getAvatarTextColor(bgColor: string): string {
    if (bgColor.includes('eff6ff')) return '#2563eb';
    if (bgColor.includes('fdf2f8')) return '#db2777'; 
    if (bgColor.includes('ecfdf5')) return '#059669';
    if (bgColor.includes('fffbeb')) return '#d97706';
    if (bgColor.includes('f3e8ff')) return '#7c3aed';
    return '#0891b2';
  }

  getProgressColor(progress: number): string {
    if (progress >= 80) return '#10b981';
    if (progress >= 50) return '#f97316';
    return '#ef4444';
  }
}