$ ->
	form = $ '.model-input-params form'
	activeFilters = $('<input type="hidden" name="active-filters" id="active-filters"/>')
	form.append activeFilters