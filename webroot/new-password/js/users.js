'use strict';

/* App Module */

var userManagement = angular.module('userManagement', []);

userManagement.factory('UserManager', ['$http','$rootScope', function($http,$rootScope) {
  var UserManager = {
	isLogged: false,
	username: '',
    userId: null,
	sessionId: null,
	userData: null,
	userConnectionMessage: 'User NOT connected',
	baseURL: ''
	//baseURL: 'http://localhost:8080/'	
	//baseURL: 'https://vertx-leancrowds.rhcloud.com/'
	};
  if (localStorage.sessionId) {
	UserManager.isLogged = true;
	UserManager.sessionId = localStorage.sessionId;
	UserManager.username = localStorage.username;
	UserManager.userId = localStorage.userId;
	UserManager.userData = angular.fromJson(localStorage.userData);
	$http.defaults.headers.common.mc_authorization = 'uId,uName,sId ' + UserManager.userId + ',' + UserManager.username + ',' + UserManager.sessionId;
	$http({method: 'POST', url: UserManager.baseURL + 'users/authorise', data: {'sessionID':UserManager.sessionId},
                 headers: {'Content-Type': 'text/plain'} }).
            success(function(data, status, headers, config) {
                if (data.status == 'ok') {
                	if (UserManager.username.toUpperCase() != data.username.toUpperCase()) UserManager.setLoginState(false,data);
                	else UserManager.userConnectionMessage = 'User connected';
                  }
                if (data.status == 'denied') {
                	UserManager.setLoginState(false,data);
                  }
            }).
            error(function(data, status, headers, config) {
		    UserManager.setLoginState(false,data);
         });
  }
  UserManager.setLoginState = function(isLogged,data) {
	if (isLogged) {
        this.isLogged = true;
        this.sessionId = data.sessionID;
		this.username = data.username;
		this.userId = data.userId;
		this.userData = data.user;
		this.userConnectionMessage = 'User connected';
		$http.defaults.headers.common.mc_authorization = 'uId,uName,sId ' + this.userId + ',' + this.username + ',' + this.sessionId;
		localStorage.sessionId = this.sessionId;
		localStorage.username = this.username;
		localStorage.userId = this.userId;
		localStorage.userData = angular.toJson(this.userData);
	} else {
        this.isLogged = false;
        this.sessionId = null;
		this.username = '';
		this.userId = null;
		this.userData = null;
		this.userConnectionMessage = 'User NOT connected';
		delete $http.defaults.headers.common.mc_authorization;
		localStorage.removeItem("sessionId");
		localStorage.removeItem("username");
		localStorage.removeItem("userId");
		localStorage.removeItem("userData");
	}
	$rootScope.userManager = this;
  }
      UserManager.login = function (username,password) {
          $http({method: 'POST', url: UserManager.baseURL + 'users/login', data: {'username':username,'password':password},
                 headers: {'Content-Type': 'text/plain'} }).
            success(function(data, status, headers, config) {
                if (data.status == 'ok') {
                	UserManager.setLoginState(true,data);                	
                }
                if (data.status == 'denied') {
                	UserManager.setLoginState(false,data);
                	UserManager.userConnectionMessage = "Username or password are not correct";
                  }
            }).
            error(function(data, status, headers, config) {
            	UserManager.setLoginState(false,data);
            	if (data == "not found") UserManager.userConnectionMessage = "Username or password are not correct";
            	if (data == "not activated") UserManager.userConnectionMessage = "User not activated"; 
            });
        };
      UserManager.logout = function () {
          $http({method: 'POST', url: UserManager.baseURL + 'users/logout', data: {'sessionID':$rootScope.userManager.sessionId},
                 headers: {'Content-Type': 'text/plain'} }).
            success(function(data, status, headers, config) {
		    UserManager.setLoginState(false,data);
            }).
            error(function(data, status, headers, config) {
		    UserManager.setLoginState(false,data);
            });
        };
      UserManager.loadUserData = function () {
    	  if ($rootScope.userManager.userId == null) return;
 	    $http.get(config.baseURL + 'rest/users/' + $rootScope.userManager.userId).
          success(function(data, status, headers, config) {
            $rootScope.userManager.userData = data;
          }).
          error(function(data, status, headers, config) {
        	UserManager.setLoginState(false,data);
        	$rootScope.errorData = data;          
          });
	  };
      UserManager.enterMasquaredMode = function (mId) {
   		$http.get('rest/users/' + mId).
        success(function(data, status, headers, config) {
        	UserManager.loggedinId = UserManager.userId;
        	UserManager.userId = mId;
        	UserManager.loggedinUsername = UserManager.username;
        	UserManager.username = data.username;
        	UserManager.userData = data;
          $http.defaults.headers.common.mc_authorization = 'uId,uName,sId,mId ' + UserManager.loggedinId + ',' + UserManager.loggedinUsername +
          ',' + UserManager.sessionId + ',' + mId;
        }).
        error(function(data, status, headers, config) {
          $scope.errorData = data;          
        });	    	      	  
      }

  $rootScope.userManager = UserManager;
  return UserManager;
  
}]);

userManagement.controller('UsersAdminController', ['$scope', '$rootScope', '$http', 'UserManager',
                                                    function($scope, $rootScope, $http, UserManager) {
 	$scope.loadAllUsers = function() {
 		$http.get('rest/users').
         success(function(data, status, headers, config) {
           $scope.allUsers = data;
         }).
         error(function(data, status, headers, config) {
           $scope.errorData = data;          
         });	 
		$http.get('rest/users?usersActivations').
        success(function(data, status, headers, config) {
          $scope.allUsersActivations = data;
          var userActivationByUsername = {}
          for(var i = 0; i < $scope.allUsersActivations.length; i++) {
       	 	var ua = $scope.allUsersActivations[i];       	 	
       	 	userActivationByUsername[ua.username] = ua;
     	  }
          $scope.userActivationByUsername = userActivationByUsername;
        }).
        error(function(data, status, headers, config) {
          $scope.errorData = data;          
        });	    	  		
     };
     $scope.toUppercase = function(a) {
    	 return a.toUpperCase();
     }
     $scope.registerUser = function() {
    	 var submitURL = 'rest/users/';
    	 var submitJson = {username:$scope.username, email:$scope.email, password:$scope.password}
    	 if ($rootScope.userManager.userId == null) { 
    		 submitURL = 'users/register';
    		 submitJson.recaptchaData = {challenge:"c",response:"r"};
    	 }
     	$http.post(submitURL,submitJson).
         success(function(data, status, headers, config) {
           $scope.userRegisterResult = data;
           $scope.loadAllUsers();
           if ($rootScope.userManager.userId == null) 
        	   window.alert('User registered successfully (anonymous registration)');
           else window.alert('User registered successfully (admin registration)');
         }).
         error(function(data, status, headers, config) {
       	  window.alert('Error in registering the user');
       	  if (status == '401') $scope.userManager.setLoginState(false,data);
       	  $scope.errorData = data;          
         });	    	
     }
     $scope.updateUserPassword = function() {
     	$http({method: 'PATCH', url: '/rest/users/' + $scope.UPuserId, data: {username:$scope.UPusername,newPassword:$scope.newPassword, isActive:$scope.isActive} }).
         success(function(data, status, headers, config) {
         	$scope.userUpdateResult = data;
         	$scope.loadAllUsers();
         	window.alert('User password updated successfully');
         }).
         error(function(data, status, headers, config) {
         	$scope.errorData = data;      
         });    	
     }
     $scope.constructDatetime = function(dateISO) {
     	var d = Date.parse(dateISO.substring(0,19));
     	return d;
     }    
     
     $scope.loadAllUsers();
 }]);

userManagement.controller('UserController', ['$scope','$http', '$location', 'UserManager','$rootScope',
                                            function($scope, $http, $location, UserManager, $rootScope) {
			$scope.login = function() {
				$scope.userManager.login($scope.username, $scope.password);
			};
			$scope.logout = function() {
				$scope.userManager.logout();
			};
			$rootScope.gotoLocation = function($path) {
				$location.url($path);
			};
			$rootScope.isRole = function(role) {
				if (!$scope.userManager.isLogged)
					return false;
				if ($scope.userManager.userData.roles.indexOf(role) > -1)
					return true;
				else
					return false;				
			}
			$rootScope.isAdmin = function() {
				if (!$scope.userManager.isLogged)
					return false;
				if ($scope.userManager.userData.roles.indexOf("admin") > -1)
					return true;
				else
					return false;
			}
			$rootScope.isSeller = function() {
				return $scope.isRole("seller");
			}
			$rootScope.isSaleManager = function() {
				return $scope.isRole("saleManager");
			}			
			$scope.enterMasquaredMode = function() {
				if ($scope.isAdmin()) $scope.userManager.enterMasquaredMode($scope.mId);
			}
}]);

userManagement.controller('NewPasswordController', ['$scope','$http', '$location', 'UserManager',
                                             function($scope, $http, $location, UserManager) {
	$scope.getParameterByName = function(name, url) {
	    if (!url) url = window.location.href;
	    name = name.replace(/[\[\]]/g, "\\$&");
	    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
	        results = regex.exec(url);
	    if (!results) return null;
	    if (!results[2]) return '';
	    return decodeURIComponent(results[2].replace(/\+/g, " "));
	}
	$scope.newPassword = function() {
		if ($scope.password != $scope.confirmPassword) {
			alert("No match between passwords");
			return;
		}
		var $id = $scope.getParameterByName("id");		
     	$http.post('users/new-password',{id:$id,password:$scope.password}).
        success(function(data, status, headers, config) {
	      window.alert('Password updated successfully');
        }).
        error(function(data, status, headers, config) {
      	  window.alert('Error updating password');
      	  $scope.errorData = data;          
        });	    	
	}
 }]);

