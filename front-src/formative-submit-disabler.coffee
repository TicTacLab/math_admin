$ ->
  btns = $ '.model-input-params .form-control.btn.btn-primary'
  btns.click ->
    btns.prop 'disabled', true
    true