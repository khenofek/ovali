'use strict';

/* App Module */

var restAdminApp = angular.module('restAdminApp', ['userManagement']);

restAdminApp.controller('RestAdminController', ['$scope', '$http',
  function($scope,$http) {
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
