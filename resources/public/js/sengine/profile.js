(function() {
var parts = window.location.pathname.split('/');
var fileId = parts[3];
var eventId = parts[5];
var url = '/sengine/files/' + fileId + '/profile/' + eventId + '/destroy';
$('#destroy-session').attr('action', url);

})();
