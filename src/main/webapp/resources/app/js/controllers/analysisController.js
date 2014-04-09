'use strict';
define(['underscore'], function() {
  var dependencies = ['$scope', '$stateParams', '$q', '$window', '$location',
    'ProjectsResource', 'AnalysisResource', 'OutcomeResource', 'InterventionResource',
    'Select2UtilService', 'TrialverseStudyResource', 'ProblemResource', 'AnalysisService'
  ];
  var AnalysisController = function($scope, $stateParams, $q, $window, $location,
    ProjectsResource, AnalysisResource, OutcomeResource, InterventionResource,
    Select2UtilService, TrialverseStudyResource, ProblemResource, AnalysisService) {

    $scope.loading = {
      loaded: false
    };

    $scope.editMode = {
      disableEditing: true
    };

    $scope.project = ProjectsResource.get($stateParams);
    $scope.analysis = AnalysisResource.get($stateParams);

    $scope.outcomes = OutcomeResource.query($stateParams);
    $scope.interventions = InterventionResource.query($stateParams);

    $scope.selectedOutcomeIds = [];
    $scope.selectedInterventionIds = [];

    $q.all([
      $scope.project.$promise,
      $scope.analysis.$promise
    ]).then(function() {
      $scope.loading.loaded = true;

      var userIsOwner = $window.config.user.id === $scope.project.owner.id;
      $scope.editMode.disableEditing = !userIsOwner || $scope.analysis.problem;

      $scope.select2Options = {
        'readonly': $scope.editMode.disableEditing
      };

      //  angular ui bug work-around, select2-ui does not properly watch for changes in the select2-options 
      $('#criteriaSelect').select2('readonly', $scope.editMode.disableEditing);
      $('#interventionsSelect').select2('readonly', $scope.editMode.disableEditing);

      $scope.studies = TrialverseStudyResource.query({
        id: $scope.project.trialverseId
      });
      $scope.selectedOutcomeIds = Select2UtilService.objectsToIds($scope.analysis.selectedOutcomes);
      $scope.selectedInterventionIds = Select2UtilService.objectsToIds($scope.analysis.selectedInterventions);

      $scope.$watchCollection('selectedOutcomeIds', function(newValue) {
        if (newValue.length !== $scope.analysis.selectedOutcomes.length) {
          $scope.analysis.selectedOutcomes = Select2UtilService.idsToObjects($scope.selectedOutcomeIds, $scope.outcomes);
          $scope.analysis.$save();
        }
      });

      $scope.$watchCollection('selectedInterventionIds', function(newValue) {
        if (newValue.length !== $scope.analysis.selectedInterventions.length) {
          $scope.analysis.selectedInterventions = Select2UtilService.idsToObjects($scope.selectedInterventionIds, $scope.interventions);
          $scope.analysis.$save();
        }
      });

      $scope.$watch('analysis.studyId', function(newValue, oldValue) {
        if (oldValue !== newValue) {
          $scope.analysis.$save();
        }
      });

      $scope.createProblem = function() {
        AnalysisService
          .createProblem($scope.analysis)
          .then(function(newUrl) {
            $location.url(newUrl);
          });
      };
    });
  };
  return dependencies.concat(AnalysisController);
});