'use strict';
define([],
  function() {
    var dependencies = ['$q', 'OutcomeService'];
    var PopulationCharacteristicService = function($q, OutcomeService) {

      var TYPE = 'ontology:PopulationCharacteristic';

      function queryItems() {
        return OutcomeService.queryItems(function(node) {
          return node['@type'] === TYPE;
        });
      }

      function addItem(item) {
        return OutcomeService.addItem(item, TYPE);
      }

      function deleteItem(item) {
        return OutcomeService.deleteItem(item);
      }

      function editItem(item) {
        return OutcomeService.editItem(item, TYPE);
      }

      return {
        queryItems: queryItems,
        addItem: addItem,
        deleteItem: deleteItem,
        editItem: editItem,
        TYPE: TYPE
      };
    };
    return dependencies.concat(PopulationCharacteristicService);
  });
