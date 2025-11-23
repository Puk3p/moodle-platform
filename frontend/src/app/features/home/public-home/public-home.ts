import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-public-home',
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule
  ],
  templateUrl: './public-home.html',
  styleUrl: './public-home.scss',
})
export class PublicHomeComponent {

}
