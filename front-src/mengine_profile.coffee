app = angular.module('MathAdmin')

app.filter 'format_timer', ->
  (timer) ->
    if timer > 1
      Math.round timer
    else
      String.fromCharCode("0x2264") + " 1"

app.filter 'format_if_float', ->

  isNeedRounding = (number, digits) ->
    roundTo = 10 ** digits
    number * roundTo % roundTo isnt 0

  (number, digits) ->
    if typeof number is 'number' and isNeedRounding(number, digits)
      number.toFixed digits
    else
      number

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

  $scope.activeFilter = window.prevActiveFilters
  $scope.$watchCollection('activeFilter', -> $('#active-filters').val angular.toJson $scope.activeFilter)
  
  $scope.filters = createFilters($scope.out)
