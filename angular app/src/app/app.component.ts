import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';


interface UploadResponse {
  uploadStatus: string;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  issueType: string;
  panNo: string;
  selectedFile: File;
  uploadStatus: string;

  private subscription: Subscription;

  constructor(private http: HttpClient) {}

  uploadFile(): void {
    const formData = new FormData();
    formData.append('issueType', this.issueType);
    formData.append('panNo', this.panNo);
    formData.append('file', this.selectedFile, this.selectedFile.name);

      this.http.post<UploadResponse>('http://localhost:8080/api/files/upload', formData)
      .subscribe({
        next: (response: UploadResponse) => {
          console.log(response);
          this.uploadStatus = response.uploadStatus;
        },
        error: (error: any) => {
          console.error(error);
          this.uploadStatus = 'Error occurred while uploading the file.';
        }
      });
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  onFileChange(event: any): void {
    this.selectedFile = event.target.files[0];
  }

}