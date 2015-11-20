$ ->
  btns = $ '.model-input-params .form-control.btn.btn-primary'
  form = $ '.model-input-params form'
  form.submit ->
    btns.prop 'disabled', true
    true