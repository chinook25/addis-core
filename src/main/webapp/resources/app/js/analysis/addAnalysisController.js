'use strict';
define(['lodash'], function(_) {
  var dependencies = ['$scope', '$state', '$modalInstance', 'AnalysisResource', 'CacheService', 'ANALYSIS_TYPES'];
  var AddAnalysisController = function($scope, $state, $modalInstance, AnalysisResource, CacheService, ANALYSIS_TYPES) {
    $scope.checkForDuplicateAnalysisName = checkForDuplicateAnalysisName;
    $scope.cancel = cancel;
    $scope.addAnalysis = addAnalysis;

    $scope.newAnalysis = {};
    $scope.duplicateAnalysisName = {
      isDuplicate: false
    };
    $scope.analysisTypes = ANALYSIS_TYPES;
    $scope.isAdding = false;

    function addAnalysis(newAnalysis) {
      $scope.isAdding = true;
      newAnalysis.projectId = $scope.project.id;
      AnalysisResource
        .save(newAnalysis)
        .$promise.then(function(savedAnalysis) {
          CacheService.evict('analysesPromises', $scope.project.id);
          $modalInstance.close();
          goToAnalysis(savedAnalysis.id, savedAnalysis.analysisType);
        });
    }

    function goToAnalysis(analysisId, analysisTypeLabel) {
      var analysisType = _.find(ANALYSIS_TYPES, function(type) {
        return type.label === analysisTypeLabel;
      });
      //todo if analysis is gemtc type and has a problem go to models view
      $state.go(analysisType.stateName, {
        userUid: $scope.userId,
        projectId: $scope.project.id,
        analysisId: analysisId
      });
    }

    function checkForDuplicateAnalysisName(title) {
      $scope.duplicateAnalysisName.isDuplicate = _.find($scope.analyses, function(item) {
        return item.title === title;
      });
    }

    function cancel() {
      $modalInstance.close();
    }

  };
  return dependencies.concat(AddAnalysisController);
});
