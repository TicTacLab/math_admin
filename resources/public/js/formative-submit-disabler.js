var btns = $(".model-input-params .form-control.btn.btn-primary");
btns.click(function(event) {
  btns.prop("disabled", true);
});