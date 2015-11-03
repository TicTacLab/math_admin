(function() {
var parts = window.location.pathname.split('/');
var fileId = parts[3];
var eventId = parts[5];
var destroyUrl = '/sengine/files/' + fileId + '/profile/' + eventId + '/destroy';
$('#destroy-session').attr('action', destroyUrl);
var downloadWorkbookUrl = '/sengine/files/' + fileId + '/profile/' + eventId + '/workbook';
$('#download-workbook').attr('action', downloadWorkbookUrl);

})();
