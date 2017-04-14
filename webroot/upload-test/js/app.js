'use strict';

/* App Module */

var fileUploadApp = angular.module('fileUpload', ['userManagement','ngFileUpload']);

fileUploadApp.controller('FileUploadController', ['$scope', 'Upload', '$timeout', function ($scope, Upload, $timeout) {
    $scope.uploadPic = function(file) {
    	alert("upload");
        file.upload = Upload.upload({
          url: 'http://192.168.1.10:8081/rest/events',
          data: {restAction: "create", restBody: Upload.json({id: 1, name: 'hello'}), file: file}
        });

        file.upload.then(function (response) {
          $timeout(function () {
            file.result = response.data;
          });
        }, function (response) {
          if (response.status > 0)
            $scope.errorMsg = response.status + ': ' + response.data;
        }, function (evt) {
          // Math.min is to fix IE which reports 200% sometimes
          file.progress = Math.min(100, parseInt(100.0 * evt.loaded / evt.total));
        });
    }
}]);