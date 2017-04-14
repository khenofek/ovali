import { Component } from '@angular/core';
import { Http, Response, RequestOptions, Request, RequestMethod, Headers } from '@angular/http';
import { UserService } from './user.service';

@Component({
  moduleId: module.id,
  selector: 'app-root',
  templateUrl: 'app.component.html',
  styleUrls: ['app.component.css']
})
export class AppComponent {
  methods = RequestMethod;
  methodsKeys = null;
  httpRequestParams = { method: RequestMethod.Get, path:"", body:"" };
  baseURL = "";
  res = null;
  error = null;
  user = null;

  constructor(private http: Http, private userService: UserService) {
    this.methodsKeys = Object.keys(this.methods).filter(key => { return Number(key) >= 0 });
    this.baseURL = window.location.protocol + "//" + window.location.host + "/";
  }  
  login(username,password) {
	  this.userService.login(username, password).subscribe((result) => {
	      if (result) {
	          alert(result);
	          this.user = this.userService.getUser();
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
  submit() {
	  this.res = null;
	  this.error = null;
	  let headers = new Headers();
	  if (this.userService.getUser() != null)
		  headers.append('mc_authorization', 'uId,uName,sId ' + this.userService.getUser()['@rid'].substring(1) + ',' + this.userService.getUser()['username'] + ',' +
			  this.userService.getAuthToken());
	  var options = new RequestOptions({
		  method: this.methods[this.httpRequestParams.method],
		  url: this.baseURL + this.httpRequestParams.path,
		  headers: headers,
		  body: this.httpRequestParams.body
		});
	  var req = new Request(options);
	  this.http.request(req).subscribe(
              (res) => this.res = res,
              (err) => this.error = err);
  }
}
