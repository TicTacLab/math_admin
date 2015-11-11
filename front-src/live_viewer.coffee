app = angular.module('liveViewer', [])

app.controller 'LiveViewerCtrl', ["$scope", "$http", "$interval", ($scope, $http, $interval) ->
  $scope.eventLog = []
  $scope.settlements = {header: [], body: []}
  $scope.filters = []
  $scope.activeFilter = {}

  $scope.bodyLimit = 20

  $scope.showMore = ->
    $scope.bodyLimit += 20

  getEventLog = ->
    $http.get("#{sengineAddr}/events/#{eventId}/event-log").then (res) ->
      $scope.eventLog = res.data.data

  getSettlements = ->
    $http.get("#{sengineAddr}/events/#{eventId}/settlements").then (res) ->
      data = res.data.data

      header = _.chain(data)
        .first()
        .keys()
        .compact()
        .value()

      filters = _.map(header, (h) ->
        options = _.chain(data)
          .map((data) -> data[h])
          .uniq()
          .compact()
          .map((opt) -> {label: opt, value: opt})
          .sortBy((opt) -> opt.label)
          .value()
        options.push({label: "*", value: undefined})
        {
          columnName: h,
          columnOptions: options
        }
      )

      $scope.settlements.header = header
      $scope.settlements.body = data
      $scope.filters = filters

  getEventLog()
  getSettlements()

  $interval(getEventLog, 2000)
  $interval(getSettlements, 2000)
]