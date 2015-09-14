(function() {
  var cookies = document.cookie;
  var matches = cookies.match(/csrf=([^;]*);?/);
  var token   = matches[1];
  $('form').each(function(i, rawForm) {
    var form = $(rawForm);
    if(form.attr('method').toLowerCase() === 'post') {
      var hidden = $('<input />');
      hidden.attr('type', 'hidden');
      hidden.attr('name', 'csrf');
      hidden.attr('value', token);
      form.append(hidden);
    }
  })
}());