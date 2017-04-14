import { Injectable } from '@angular/core';
import { Http, Headers } from '@angular/http';
import 'rxjs/add/operator/map';

@Injectable()
export class UserService {
  private loggedIn = false;
  private user = null;

  constructor(private http: Http) {
    this.loggedIn = !!window.localStorage.getItem('auth_token');
  }
  login(username, password) {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');

    return this.http
      .post('/users/login', JSON.stringify({ username, password }), { headers })
      .map(res => res.json())
      .map((res) => {
        if (res.status == 'ok') {
            window.localStorage.setItem('auth_token', res.sessionID);
            this.user = res.user;
            this.loggedIn = true;
        }
        return res.status;
      });
  }  
  logout() {
	window.localStorage.removeItem('auth_token');
	this.user = null;
    this.loggedIn = false;
  }
  isLoggedIn() {
    return this.loggedIn;
  }
  getAuthToken() { return window.localStorage.getItem('auth_token'); }
  getUser() { return this.user; }
}