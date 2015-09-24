(function () {
  var $passwordBad = $('#password-strength .progress-bar-danger');
  var $passwordMod = $('#password-strength .progress-bar-warning');
  var $passwordGood = $('#password-strength .progress-bar-success');
  var $passwordInputElm = $('#field-password');

  $passwordInputElm.keyup(function() {

    var pass = $passwordInputElm.val();
    $.ajax('/users/pass-analyze', {
      method: 'POST',
      data: JSON.stringify({password: pass})
    }).done(function(data) {
      var grade = data.grade;
      switch(grade) {
        case 3: $passwordBad.css("width", "33%"); $passwordMod.css("width", "33%"); $passwordGood.css("width", "34%"); break;
        case 2: $passwordBad.css("width", "33%"); $passwordMod.css("width", "33%"); $passwordGood.css("width", "0%"); break;
        case 1: $passwordBad.css("width", "33%"); $passwordMod.css("width", "0%"); $passwordGood.css("width", "0%"); break;
      }
    })
  })
})()