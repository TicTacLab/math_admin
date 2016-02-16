$ ->
  $('[data-toggle="tooltip"]').tooltip()
  $('#upload-form-container').submit ->
    $('.upload-indicator-container').show()
    console.log "hello"
    $('#upload-form-container input[type="submit"]').attr('disabled', 'disabled')