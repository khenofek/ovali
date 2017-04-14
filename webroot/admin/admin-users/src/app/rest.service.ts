import { Injectable } from '@angular/core';
import { Http, Headers } from '@angular/http';
import { UserService } from './user.service';

@Injectable()
export class RestService {
	constructor(private http: Http, private userService: UserService) {		
	}
	queryResources(resourceName: string, query:string = "", range = null) {
	    let headers = new Headers();
	    if (this.userService.getUser() != null)
	        headers.append('mc_authorization', 'uId,uName,sId ' + this.userService.getUser()['@rid'].substring(1) + ',' + this.userService.getUser()['username'] + ',' +
	              this.userService.getAuthToken());
	    let URL = '/rest/' + resourceName + '?envelop';
	    if (range != null) URL += '&Range=' + range.from + '-' + range.to;
	    return this.http.get(URL, { headers })
	      .map(res => res.json())
	      .map((res) => {
	    	  let ret: any = {};
	    	  ret[resourceName] = res.docs;
	    	  if (res.rangeHeaders) ret['itemsAmount'] = res.rangeHeaders['Content-Range'].split("/")[1];
	    	  return ret;
	      });
	}
}