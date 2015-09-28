(function() {
  var cookies = document.cookie;
  var matches = cookies.match(/csrf=([^;]*);?/);
  var token   = matches[1];
  $('form').each(function(i, rawForm) {
    var form = $(rawForm);
    var method = form.attr('method');
    if(method && method.toLowerCase() === 'post') {
      var hidden = $('<input />');
      hidden.attr('type', 'hidden');
      hidden.attr('name', 'csrf');
      hidden.attr('value', token);
      form.append(hidden);
    }
  })
}());