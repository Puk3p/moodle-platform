import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { 
  faChevronLeft, faPaperPlane, faBold, faItalic, faUnderline, 
  faListUl, faListOl, faLink, faImage, faInfoCircle 
} from '@fortawesome/free-solid-svg-icons';
import { AnnouncementsService } from '../../../core/services/announcements.service';
import { QuizzesService } from '../../../core/services/quizzes.service'; 

@Component({
  selector: 'app-create-announcement',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule],
  templateUrl: './create-announcement.html',
  styleUrls: ['./create-announcement.scss']
})
export class CreateAnnouncementComponent implements OnInit {
  private announcementService = inject(AnnouncementsService);
  private quizzesService = inject(QuizzesService);
  private location = inject(Location);
  private router = inject(Router);

  
  faChevronLeft = faChevronLeft; faPaperPlane = faPaperPlane;
  faBold = faBold; faItalic = faItalic; faUnderline = faUnderline;
  faListUl = faListUl; faListOl = faListOl; faLink = faLink; faImage = faImage;
  faInfoCircle = faInfoCircle;

  
  coursesList: any[] = [];
  
  
  announcement = {
    courseId: null as number | null,
    title: '',
    body: ''
  };

  isSubmitting = false;

  ngOnInit() {
    this.loadCourses();
  }

  loadCourses() {
    
    this.quizzesService.getAllCoursesSimple().subscribe({
      next: (data) => this.coursesList = data,
      error: (err) => console.error('Failed to load courses', err)
    });
  }

  goBack() {
    this.location.back();
  }

  publish() {
    if (!this.announcement.courseId || !this.announcement.title || !this.announcement.body) {
      alert('Please fill in all fields (Course, Title, Content).');
      return;
    }

    this.isSubmitting = true;

    const payload = {
        courseId: this.announcement.courseId,
        title: this.announcement.title,
        body: this.announcement.body
    };

    this.announcementService.createAnnouncement(payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        alert('Announcement published successfully!');
        this.goBack();
      },
      error: (err) => {
        console.error(err);
        this.isSubmitting = false;
        alert('Failed to publish announcement.');
      }
    });
  }
}