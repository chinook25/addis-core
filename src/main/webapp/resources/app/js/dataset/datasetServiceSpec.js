'use strict';
define(['angular', 'angular-mocks'], function() {
  describe('dataset service', function() {

    var mockDatasetResource;

    beforeEach(module('trialverse.dataset'));

    beforeEach(function() {
      mockDatasetResource = jasmine.createSpyObj('DatasetResource', ['query']);
      module('trialverse', function($provide) {
        $provide.value('DatasetResource', mockDatasetResource);
      });
    });

    describe('loadDatasets', function() {

      it('should query the datasetResource', inject(function(DatasetService) {
        DatasetService.loadDatasets();
        expect(mockDatasetResource.query).toHaveBeenCalled();
      }));

    });

  });
});
