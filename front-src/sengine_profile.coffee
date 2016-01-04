app = angular.module('MathAdmin')

app.controller 'SengineProfileCtrl', ($scope, predicates) ->
# {"Goal": {"Team": [...]}} -> [{"EventType": "Goal", "Team": [...]}]
  $scope.eventTypes = window.event_types

  $scope.eventLog = []
  $scope.eventLogJson = ""

  $scope.appendToEventLog = ->
    $scope.eventLog.unshift(_.chain($scope.newEvent)
                             .clone()
                             .omit(_.isNull)
                             .value())
    $scope.eventLogJson = angular.toJson $scope.eventLog, null, 2

  $scope.removeFromEventLog = (index) ->
    $scope.eventLog.splice(index, 1)
    $scope.eventLogJson = angular.toJson $scope.eventLog, null, 2

  $scope.cleanNewEvent = (eventType)->
    $scope.newEvent = EventType: eventType # remove all but event type

  $scope.getEnumerableAttributes = _.memoize (eventType) ->
    _.chain($scope.eventTypes[eventType])
     .pairs()
     .filter(_.negate(predicates.isNumericAttribute))
     .sortBy(_.first)
     .map((values) -> _.zipObject(["name", "values"], values))
     .value()

  $scope.getNumericAttributeNames = _.memoize (eventType) ->
    _.chain($scope.eventTypes[eventType])
     .pairs()
     .filter(predicates.isNumericAttribute)
     .sortBy(_.first)
     .map((values) -> _.zipObject(["name", "values"], values))
     .value()



app.factory 'predicates', ->
  isNumericAttribute: ([attr, values]) -> _.first(values) == 'Numeric'

app.filter 'eventTypeNames', ->
  _.memoize (eventTypes) ->
        _.chain(eventTypes)
         .keys()
         .sortBy(_.indentity)
         .value()

