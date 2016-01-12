app = angular.module('MathAdmin')

app.controller 'SengineProfileCtrl', ['$scope', 'predicates', ($scope, preds) ->
      # {"Goal": {"Team": [...]}} -> [{"EventType": "Goal", "Team": [...]}]
      $scope.eventTypes = _.map(window.event_types, (value, key) ->
                                   value.EventType = key
                                   value)

      $scope.eventLog = []
      $scope.eventLogJson = ""
      $scope.serverEventLogLength = event_log_length
      $scope.isAppendEvents = true

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

      filterAttributes = (predicate, eventType) ->
        _.chain($scope.eventTypes)
         .find(_.matchesProperty('EventType', eventType))
         .omit(['EventType', '$$hashKey'])
         .pairs()
         .filter(predicate)
         .sortBy(_.first)
         .map((values) -> _.zipObject(["name", "values"], values))
         .value()

      $scope.getEnumerableAttributes = _.memoize _.partial filterAttributes, _.negate preds.isNumericAttribute
      $scope.getNumericAttributes = _.memoize _.partial filterAttributes, preds.isNumericAttribute
      $scope.getEventIndex = (isApeendEvents, eventLogLength, eventIndex, serverEventLogLength) ->
        console.log isApeendEvents, eventLogLength, eventIndex, serverEventLogLength
        if isApeendEvents
          serverEventLogLength + eventLogLength - eventIndex
        else
          eventLogLength - eventIndex
  ]

app.factory 'predicates', ->
  isNumericAttribute: ([attr, values]) -> _.first(values) == 'Numeric'


