'use strict';

/* App Module */

var restAdminApp = angular.module('restAdminApp', ['userManagement']);

restAdminApp.controller('RestAdminController', ['$scope', '$http',
  function($scope,$http) {
    $scope.submit = function () {
        $scope.submited = 'submited: ' + $scope.method + ' ' + $scope.url;
        $http({method: $scope.method, url: $scope.url, data: $scope.postdata,
               headers: {'Content-Type': 'text/plain', 'mc-sessionid': $scope.sessionId} }).
          success(function(data, status, resp_headers, config) {
            $scope.status = status;
            $scope.data = data;
	    $scope.headers = resp_header;
          }).
          error(function(data, status, headers, config) {
            $scope.status = status;
            $scope.data = data;          
          });
      };
      $scope.contact = function() {
    		$http.post('http://localhost:8080/users/contact',{name:$scope.name, email:$scope.email, message:$scope.message}).
  		success(function(data, status, headers, config) {
  			alert("Message was sent");
  			$scope.contactSent = true;
  		}).
  		error(function(data, status, headers, config) {
  			alert("Contact Error");
  			$scope.errorData = data;          
  		});
      };
  }]);
