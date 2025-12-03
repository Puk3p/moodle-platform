import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-public-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './public-home.html',
  styleUrl: './public-home.scss',
})
export class PublicHomeComponent {

  activeIndex = 0;

  tracks = [
    {
      title: 'Programming Essentials',
      tag: 'Year 1 • Basics',
      description: 'Learn the foundations of programming in C/C++/Java: variables, control flow, functions, debugging și bune practici.',
      skills: ['Syntax & logic', 'Debugging', 'Clean code'],
      icon: 'fa-code',
      color: '#ff5200',
    },
    {
      title: 'Web Development',
      tag: 'Frontend & Backend',
      description: 'De la HTML/CSS/JS la API-uri REST, autentificare și baze de date. Tot ce ai nevoie pentru proiectele de la laborator.',
      skills: ['HTML / CSS', 'Angular / React basics', 'REST APIs'],
      icon: 'fa-globe',
      color: '#4b7cf5',
    },
    {
      title: 'Algorithms & Data Structures',
      tag: 'Exam Power Pack',
      description: 'Liste, stive, cozi, arbori, grafuri și algoritmi clasici explicați cu exemple și probleme rezolvate.',
      skills: ['Complexity', 'Trees & graphs', 'Problem solving'],
      icon: 'fa-diagram-project',
      color: '#22c55e',
    },
    {
      title: 'Databases & Cloud',
      tag: 'SQL • NoSQL • Cloud',
      description: 'Concepte de baze de date relaționale, interogări SQL, modelare de date și introducere în servicii cloud.',
      skills: ['SQL', 'Entity design', 'Cloud basics'],
      icon: 'fa-database',
      color: '#eab308',
    },
  ];

  get currentTrack() {
    return this.tracks[this.activeIndex];
  }

  next(): void {
    this.activeIndex = (this.activeIndex + 1) % this.tracks.length;
  }

  prev(): void {
    this.activeIndex =
      (this.activeIndex - 1 + this.tracks.length) % this.tracks.length;
  }

  goTo(index: number): void {
    this.activeIndex = index;
  }
}
