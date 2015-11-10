getEventLog = ->
  $.get("#{sengineAddr}/events/#{eventId}/event-log", (res) ->
    eventLogStrs = _.map(res.data, JSON.stringify)
    source = $('#event-log-tpl').html()
    template = Handlebars.compile(source)
    eventLogHtml = template(eventLog: eventLogStrs)
    $('#event-log').html(eventLogHtml)
  )

getSettlements = ->
  $.get("#{sengineAddr}/events/#{eventId}/settlements", (res) ->
    header = _.chain(res.data)
              .first()
              .keys()
              .value()

    settlements = _.map(res.data, (settlement) ->
      _.map(header, (h) -> settlement[h])
    )

    source = $('#settlements-tpl').html()
    template = Handlebars.compile(source)
    settlementsHtml = template(header: header, settlements: settlements)
    $('#settlements').html(settlementsHtml)
  )

$ ->
  getEventLog()
  getSettlements()

  setInterval(getEventLog, 2000)
  setInterval(getSettlements, 2000)
