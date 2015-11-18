app = angular.module('MathAdmin')

app.controller 'MengineProfileCtrl', ($scope) ->
  $scope.out = window.out
  $scope.activeFilter = {}
  $scope.rowLimit = 50

  $scope.showMore = ->
    $scope.rowLimit += 50

  $scope.showAll = ->
    $scope.rowLimit = $scope.out.values.length

  $scope.isFormVisible = true
  $scope.toggleForm = ->
    $scope.isFormVisible = !$scope.isFormVisible

  createFilters = (out) ->
    values = out.values
    header = out.header

    _.map(header, (h) ->
      options = _.chain(values)
        .map((row) -> row[h])
        .uniq()
        .compact()
        .map((opt) -> {label: opt, value: opt})
        .sortBy((opt) -> opt.label)
        .value()
      options.unshift({label: '*', value: undefined})
      options
    )

  $scope.filters = createFilters($scope.out)
