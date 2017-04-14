import { Component } from '@angular/core';
import { Http, Response, RequestOptions, Request, RequestMethod, Headers } from '@angular/http';
import { UserService } from './user.service';
import { RestService } from './rest.service';

@Component({
  moduleId: module.id,
  selector: 'app-root',
  templateUrl: 'app.component.html',
  styleUrls: ['app.component.css'],
  providers: [RestService]
})
export class AppComponent {
  user = null;
  users = null;
  itemsAmount = null;
  itemsPerPage = 50;
  pages = [];

  constructor(private http: Http, private userService: UserService, private restService: RestService) {	  
  }  
  login(username,password) {
	  this.userService.login(username, password).subscribe((result) => {
	      if (result) {
	          alert(result);
	          this.user = this.userService.getUser();
	          this.loadUsersPage(0);
	        }
	  },
	  (err) => { 
		  this.user = null;
		  this.userService.logout();
		  alert(err)
	  });
  }
  logout() {
	  this.userService.logout();
	  this.user = null;
  }
  loadUsersPage(page) {
	  this.restService.queryResources("users", "", {from:page*this.itemsPerPage,to:(page+1)*this.itemsPerPage-1}).subscribe((res) => {
          this.users = res.users;
          if (this.itemsAmount != res.itemsAmount) {
              this.itemsAmount = res.itemsAmount;
              var numberOfPages = Math.ceil(this.itemsAmount/this.itemsPerPage);
              for (var n = 0; n < numberOfPages; n++) this.pages.push(n);         	  
          }
      });
  }
}
