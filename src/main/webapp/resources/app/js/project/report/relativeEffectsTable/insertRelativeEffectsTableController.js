'use strict';
define(['lodash'], function(_) {
  var dependencies = ['$scope', '$stateParams', '$modalInstance', '$q', 'AnalysisResource',
    'ReportDirectiveService', 'callback', 'CacheService'
  ];
  var InsertRelativeEffectsTableController = function($scope, $stateParams, $modalInstance, $q, AnalysisResource,
    ReportDirectiveService, callback, CacheService) {
    var analysesPromise = CacheService.getAnalyses($stateParams);
    var modelsPromise = CacheService.getModelsByProject($stateParams);

    $scope.selections = {};
    $scope.isRegressionModel = false;
    $scope.selectedAnalysisChanged = selectedAnalysisChanged;
    $scope.selectedModelChanged = selectedModelChanged;
    $scope.insertRelativeEffectsTable = insertRelativeEffectsTable;
    $scope.loading = {
      loaded: false
    };

    $q.all([analysesPromise, modelsPromise]).then(function(values) {
      var analyses = values[0];
      var models = values[1];
      models = ReportDirectiveService.getNonNodeSplitModels(models);
      $scope.analyses = ReportDirectiveService.getDecoratedSyntheses(analyses, models, []);
      $scope.selections.analysis = $scope.analyses[0];
      $scope.selectedAnalysisChanged();
      $scope.loading.loaded = true;
    });

    function selectedAnalysisChanged() {
      if (!$scope.selections.analysis) {
        return;
      }
      if ($scope.selections.analysis.primaryModel) {
        $scope.selections.model = _.find($scope.selections.analysis.models, ['id', $scope.selections.analysis.primaryModel]);
      } else {
        $scope.selections.model = $scope.selections.analysis.models[0];
      }
      selectedModelChanged();
    }

    function selectedModelChanged() {
      if (!$scope.selections.model) {
        return;
      }
      $scope.isRegressionModel = $scope.selections.model.modelType.type === 'regression';
      if ($scope.isRegressionModel && $scope.selections.model.regressor.levels.length) {
        $scope.selections.regressionLevel = $scope.selections.model.regressor.levels[0];
      } else {
        delete $scope.selections.regressionLevel;
      }
    }

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    function insertRelativeEffectsTable() {
      callback(ReportDirectiveService.getDirectiveBuilder('relative-effects-table')($scope.selections.analysis.id,
        $scope.selections.model.id, $scope.selections.regressionLevel));
      $modalInstance.close();
    }
  };
  return dependencies.concat(InsertRelativeEffectsTableController);
});