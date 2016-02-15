$ ->
  $('#upload-form-container').submit ->
    $('#upload-indicator-container').show()
    $('#upload-form-container input[type="submit"]').attr('disabled', 'disabled')